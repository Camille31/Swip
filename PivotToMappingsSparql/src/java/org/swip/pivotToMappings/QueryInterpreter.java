package org.swip.pivotToMappings;

import org.apache.log4j.Logger;
import org.swip.utils.sparql.SparqlClient;

public class QueryInterpreter extends Thread {

    private static final Logger logger = Logger.getLogger(QueryInterpreter.class);
    String queryUri = null;
    SparqlClient sparqlClient = null;
    boolean useFederatedSparql = false;
    boolean useLarq = true;
    String kbLocation = null;
    String queriesNamedGraphUri = null;
    String patternsNamedGraphUri = null;
    int numMappings = 0;

    public QueryInterpreter(String queryUri, SparqlClient sparqlServer, boolean useFederatedSparql, boolean useLarq, String kbLocation, String queriesUri, String patternsUri, int numMappings) {
        this.queryUri = queryUri;
        this.sparqlClient = sparqlServer;
        this.useFederatedSparql = useFederatedSparql;
        this.useLarq = useLarq;
        this.kbLocation = kbLocation;
        this.queriesNamedGraphUri = queriesUri;
        this.patternsNamedGraphUri = patternsUri;
        this.numMappings = numMappings;
    }

    @Override
    public void run() {
        boolean commitUpdate = true;
        boolean logQuery = false;
        logger.info("================================================================");
        logger.info("Matching query elements to knowledge base elements:");
        logger.info("---------------------------------------------------\n");
        performMatching(queryUri, sparqlClient, commitUpdate, logQuery);
        logger.info("================================================================");
        logger.info("Mapping patterns elements:");
        logger.info("--------------------------\n");
        performElementMapping(queryUri, sparqlClient, commitUpdate, logQuery);
//        commitUpdate = false;
        logger.info("================================================================");
        logger.info("Mapping subpattern collections:");
        logger.info("------------------------\n");
        performSpCollectionMapping(queryUri, sparqlClient, commitUpdate, logQuery);
        logger.info("================================================================");
        logger.info("Mapping patterns:");
        logger.info("------------------------\n");
        performPatternMapping(queryUri, sparqlClient, commitUpdate, logQuery);
        logger.info("================================================================");
        logger.info("Ranking pattern mappings:");
        logger.info("------------------------\n");
        performMappingRanking(queryUri, sparqlClient, commitUpdate, logQuery);
        logger.info("================================================================");
        logger.info("Clearing KB:");
        logger.info("-----------\n");
        clearMappings(queryUri, sparqlClient, commitUpdate, logQuery);
        logger.info("================================================================");
        logger.info("Generate descriptive sentences:");
        logger.info("-----------\n");
        generateDescriptiveSentences(queryUri, sparqlClient, commitUpdate, logQuery);
        logger.info("================================================================");
        logger.info("Generate SPARQL queries:");
        logger.info("-----------\n");
        generateSparqlQueries(queryUri, sparqlClient, commitUpdate, logQuery);
        // change query processing state
        Controller.getInstance().changeQueryProcessingState(queryUri, sparqlClient, queriesNamedGraphUri, "QueryProcessed");
    }

    private void performMatching(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {
        // change query processing state
        Controller.getInstance().changeQueryProcessingState(queryUri, sparqlServer, queriesNamedGraphUri, "PerformingMatching");

        String query = "# matching keywords to KB labels\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX pf:   <http://jena.hpl.hp.com/ARQ/property#>\n"
                + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?matchUri a queries:Matching;\n"
                + "               queries:matchingHasKeyword ?keyword;\n"
                + "               queries:matchingHasResource ?r;\n"
                + "               queries:matchingHasScore ?s;\n"
                + "               queries:matchingHasMatchedLabel ?l.\n"
                + "   ?keyword queries:keywordAlreadyMatched \"true\"^^xsd:boolean.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  {\n"
                + "    # subquery with DISTINCT to solve unidentified problem apparently due to larq\n"
                + "    SELECT DISTINCT * WHERE\n"
                + "    {\n"
                + "      GRAPH <" + queriesNamedGraphUri + ">\n"
                + "      {\n"
                + "        <" + queryUri + "> queries:queryHasQueryElement ?keyword.\n"
                + "        ?keyword a queries:KeywordQueryElement;\n"
                + "                 queries:queryElementHasValue ?keywordValue.\n"
                + "        FILTER NOT EXISTS { ?keyword queries:keywordAlreadyMatched \"true\"^^xsd:boolean. }\n"
                + "      }\n";
        query += "      {\n"
                + "        " + ((useFederatedSparql) ? "SERVICE" : "GRAPH") + " <" + kbLocation + ">\n"
                + "        {\n";
        if (useLarq) {
            query += "          (?l ?score) pf:textMatch (?keywordValue 6.0 5).\n"
                    + "          ?r rdfs:label ?l.\n"
                    + "          BIND ((?score * 1) AS ?s)\n";
        } else {
            query += "          CONTAINS(?l ?keywordValue)\n"
                    + "          BIND (10 AS ?s)\n";
        }
        query += "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "  BIND (UUID() AS ?matchUri)\n"
                + "}";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }
    }

    /**
     * this step could be simply done using a reasoner and property chain axioms
     * @param queryUri
     * @param sparqlServer 
     */
    private void performElementMapping(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {
        // change query processing state
        Controller.getInstance().changeQueryProcessingState(queryUri, sparqlServer, queriesNamedGraphUri, "PerformingElementMapping");

        // mapping KB pattern elements to keywords
        String query = "# mapping KB pattern elements to keywords\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX owl:  <http://www.w3.org/2002/07/owl#>\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?emUri a queries:KeywordMapping;\n"
                + "           a queries:ElementMapping;\n" // we don't use any reasonner in queries graph
                + "           queries:emHasPatternElement ?pe;\n"
                + "           queries:mappingHasPatternConstituent ?pe;\n" // we don't use any reasonner in queries graph
                + "           queries:emHasMatching ?matching;\n"
                + "           queries:emHasQueryElement ?keyword;\n"
                + "           queries:emHasKeyword ?keyword;\n" // we don't use any reasonner in queries graph
                + "           queries:emHasMatchedLabel ?l;\n"
                + "           queries:emHasResource ?r;\n"
                + "           queries:emHasScore ?score;\n"
                + "           queries:mappingHasQuery <" + queryUri + ">;\n"
                + "           queries:hasDescriptiveSubsentence ?descSubsentUri.\n"
                + "    ?descSubsentUri a queries:DescriptiveSubsentence;\n"
                + "                   queries:dsIsQueried ?queried;\n"
                + "                   queries:hasStringValue ?stringValue.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    <" + queryUri + "> queries:queryHasQueryElement ?keyword.\n"
                + "    ?matching queries:matchingHasKeyword ?keyword;\n"
                + "              queries:matchingHasResource ?r;\n"
                + "              queries:matchingHasScore ?score;\n"
                + "              queries:matchingHasMatchedLabel ?l.\n"
                + "    OPTIONAL\n"
                + "    {\n"
                + "      ?keyword queries:queryElementIsQueried ?queried.\n"
                + "    }\n"
                + "  }\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?p patterns:patternHasPatternElement ?pe.\n"
                + "    ?pe patterns:isQualifying \"true\"^^xsd:boolean;\n"
                + "        patterns:targetsKBElement ?kbe.\n"
                + "  }\n"
                + "  # class\n"
                + "  {\n"
                + "    GRAPH <" + kbLocation + ">\n"
                + "    {\n"
                + "      {?r a owl:Class.} UNION {?r a rdfs:Class.}\n"
                + "      ?r rdfs:subClassOf* ?kbe.\n"
                + "      BIND (\"some \" AS ?stringValuePrefix)\n"
                + "    }\n"
                + "  }\n"
                + "  UNION\n"
                + "  # pseudo class (*Type properties)\n"
                + "  {\n"
                + "    GRAPH <" + queriesNamedGraphUri + ">\n"
                + "    {\n"
                + "      ?jtp a queries:JokerTypeProperty;\n"
                + "           queries:concernsProperty ?prop;\n"
                + "           queries:hasClass ?kbe;\n"
                + "           queries:hasPseudoClass ?r.\n"
                + "      BIND (\"some \" AS ?stringValuePrefix)\n"
                + "    }\n"
                + "  }\n"
                + "  UNION\n"
                + "  # property\n"
                + "  {\n"
                + "    GRAPH <" + kbLocation + ">\n"
                + "    {\n"
                + "      {?r a owl:ObjectProperty.} UNION {?r a owl:DatatypeProperty.}\n"
                + "      ?r rdfs:subPropertyOf* ?kbe.\n"
                + "      BIND (\"prop \" AS ?stringValuePrefix)\n"
                + "    }\n"
                + "  }\n"
                + "  UNION\n"
                + "  # instance\n"
                + "  {\n"
                + "    GRAPH <" + kbLocation + ">\n"
                + "    {\n"
                + "      ?r a ?c.\n"
                + "      ?c rdfs:subClassOf* ?kbe.\n"
                + "      BIND (\"inst \" AS ?stringValuePrefix)\n"
                + "    }\n"
                + "  }\n"
                + "  BIND (UUID() AS ?emUri)\n"
                + "  BIND (IRI(CONCAT(str(?emUri), \"_descSubsent\")) AS ?descSubsentUri)\n"
                + "  BIND (if (BOUND(?stringValuePrefix), CONCAT(?stringValuePrefix, ?l), STR(?l)) AS ?stringValue)\n"
                + "}";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }

        // remove "redundant" element mappings, i.e. mappings having a same pattern element and distinct KB resources with same matched label
        // FIXME: move this to the matching level
        query = "# remove \"redundant\" element mappings, i.e. mappings having a same pattern element and distinct KB resources with same matched label\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
                + "DELETE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?em2 ?a ?b.\n"
                + "    ?descSubsentUri ?c ?d;\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?em1 queries:mappingHasQuery <" + queryUri + ">;\n"
                + "         queries:emHasPatternElement ?pe;\n"
                + "         queries:emHasMatchedLabel ?l.\n"
                + "    ?em2 queries:mappingHasQuery <" + queryUri + ">;\n"
                + "         queries:emHasPatternElement ?pe;\n"
                + "         queries:emHasMatchedLabel ?l.\n"
                + "    FILTER (str(?em1) < str(?em2))\n"
                + "    ?em2 ?a ?b;\n"
                + "         queries:hasDescriptiveSubsentence ?descSubsentUri.\n"
                + "    ?descSubsentUri ?c ?d;\n"
                + "  }\n"
                + "}";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }

        // find out instance/class mappings
        query = "# find out instance/class mappings\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?emUri a queries:InstanceClassMapping;\n"
                + "           a queries:ElementMapping;\n" // we don't use any reasonner in queries graph
                + "           queries:emHasPatternElement ?pe;\n"
                + "           queries:mappingHasPatternConstituent ?pe;\n" // we don't use any reasonner in queries graph
                + "           queries:mappingHasQuery <" + queryUri + ">;\n"
                + "           queries:hasInstanceMapping ?im;\n"
                + "           queries:hasClassMapping ?cm;\n"
                + "           queries:emHasScore ?score;\n"
                + "           queries:emHasKeyword ?iKeyword, ?cKeyword;\n"
                + "           queries:emHasQueryElement ?iKeyword, ?cKeyword;\n" // we don't use any reasonner in queries graph
                + "           queries:hasDescriptiveSubsentence ?descSubsentUri.\n"
                + "    ?descSubsentUri a queries:DescriptiveSubsentence;\n"
                + "                    queries:hasStringValue ?descSubsentValue.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    # instance mapping\n"
                + "    ?im a queries:ElementMapping;\n"
                + "        queries:mappingHasQuery <" + queryUri + ">;\n"
                + "        queries:emHasPatternElement ?pe;\n"
                + "        queries:emHasResource ?ir;\n"
                + "        queries:emHasScore ?iScore;\n"
                + "        queries:emHasKeyword ?iKeyword;\n"
                + "        (queries:hasDescriptiveSubsentence/queries:hasStringValue) ?iDescSubsentValue.\n"
                + "    # class mapping\n"
                + "    ?cm a queries:ElementMapping;\n"
                + "        queries:mappingHasQuery <" + queryUri + ">;\n"
                + "        queries:emHasPatternElement ?pe;\n"
                + "        queries:emHasResource ?cr;\n"
                + "        queries:emHasScore ?cScore;\n"
                + "        queries:emHasKeyword ?cKeyword;\n"
                + "        queries:emHasMatchedLabel ?cMatchedLabel.\n"
                + "  }\n"
                + "  {\n"
                + "    GRAPH <" + kbLocation + ">\n"
                + "    {\n"
                + "      ?ir a ?cr.\n"
                + "    }\n"
                + "  }\n"
                + "  UNION\n"
                + "  {\n"
                + "    GRAPH <" + queriesNamedGraphUri + ">\n"
                + "    {\n"
                + "      ?jtp a queries:JokerTypeProperty;\n"
                + "           queries:concernsProperty ?pseudoType.\n"
                + "    }\n"
                + "    GRAPH <" + kbLocation + ">\n"
                + "    {\n"
                + "      ?ir ?pseudoType ?cr.\n"
                + "    }\n"
                + "  }\n"
                + "  BIND (UUID() AS ?emUri)\n"
                + "  BIND (IRI(CONCAT(str(?emUri), \"_descSubsent\")) AS ?descSubsentUri)\n"
                + "  BIND (CONCAT( ?iDescSubsentValue, \" (\", ?cMatchedLabel, \")\") AS ?descSubsentValue)\n"
                + "  BIND (?iScore + ?cScore AS ?score)\n"
                + "}";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }

        // add an empty mapping to all qualifying pattern elements
        query = "# add an empty mapping to all pattern elements\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?emUri a queries:EmptyElementMapping;\n"
                + "           a queries:ElementMapping;\n" // we don't use any reasonner in queries graph
                + "           queries:emHasPatternElement ?pe;\n"
                + "           queries:mappingHasPatternConstituent ?pe;\n" // we don't use any reasonner in queries graph
                + "           queries:mappingHasQuery <" + queryUri + ">;\n"
                + "           queries:emHasScore \"0\"^^xsd:float;\n"
                + "           queries:hasDescriptiveSubsentence ?descSubsentUri.\n"
                + "    ?descSubsentUri a queries:DescriptiveSubsentence;\n"
                + "                   queries:hasStringValue ?l.\n"
                + "    ?pe queries:toConsiderInMappingQuery <" + queryUri + ">.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pe a patterns:PatternElement;\n"
                + "        patterns:isQualifying \"true\"^^xsd:boolean;\n"
                + "        patterns:targetsKBElement ?kbe.\n"
                + "    OPTIONAL { ?kbe rdfs:label ?label. }\n"
                + "  }\n"
                + "  BIND (UUID() AS ?emUri)\n"
                + "  BIND (IRI(CONCAT(str(?emUri), \"_descSubsent\")) AS ?descSubsentUri)\n"
                + "  BIND (IF( BOUND(?label), CONCAT( \"(a/an)\", ?label), CONCAT( \"[\", STR(?kbe), \"]\")) AS ?l)\n"
                + "}";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }

        // mapping literals
        query = "# mapping literals\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?emUri a queries:LiteralElementMapping;\n"
                + "           a queries:ElementMapping;\n" // we don't use any reasonner in queries graph
                + "           queries:emHasPatternElement ?pe;\n"
                + "           queries:mappingHasPatternConstituent ?pe;\n" // we don't use any reasonner in queries graph
                + "           queries:emHasLiteralQe ?literalQe;\n"
                + "           queries:emHasQueryElement ?literalQe;\n" // we don't use any reasonner in queries graph
                + "           queries:emHasScore \"15\"^^xsd:float;\n"
                + "           queries:mappingHasQuery <" + queryUri + ">;\n"
                + "           queries:hasDescriptiveSubsentence ?descSubsentUri.\n"
                + "    ?descSubsentUri a queries:DescriptiveSubsentence;\n"
                + "                    queries:dsIsQueried ?queried;\n"
                + "                    queries:hasStringValue ?stringValue.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    <" + queryUri + "> queries:queryHasQueryElement ?literalQe.\n"
                + "    ?literalQe a queries:LiteralQueryElement;\n"
                + "               queries:literalQueryElementHasType ?swipTypeString;\n"
                + "               queries:queryElementHasValue ?literalValue.\n"
                + "    ?lmr a queries:LiteralMappingRule;\n"
                + "         queries:hasSwipTypeString ?swipTypeString;\n"
                + "         queries:hasLiteralType ?literalType.\n"
                + "    OPTIONAL\n"
                + "    {\n"
                + "      ?literalQe queries:queryElementIsQueried ?queried.\n"
                + "    }\n"
                + "  }\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?p patterns:patternHasPatternElement ?pe.\n"
                + "    ?pe patterns:targetsLiteralType ?literalType.\n"
                + "  }\n"
                + "  BIND (UUID() AS ?emUri)\n"
                + "  BIND (IRI(CONCAT(str(?emUri), \"_descSubsent\")) AS ?descSubsentUri)\n"
                + "  BIND ( IF (?literalValue=\"?\", CONCAT (\"some \", ?swipTypeString), ?literalValue) AS ?stringValue)\n"
                + "}";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }

    }

    private void performSpCollectionMapping(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {
        // change query processing state
        Controller.getInstance().changeQueryProcessingState(queryUri, sparqlServer, queriesNamedGraphUri, "PerformingSpCollectionMapping");

        initializeSpCollectionMapping(queryUri, sparqlServer, commitUpdate, logQuery);

        int iteration = 0;
        do {
//        for (int i = 0; i < 2; i++) {
            logger.info("iteration " + ++iteration);
            if (iteration > 1) {
                removeMappingsOfEmptyMappings(queryUri, sparqlServer, commitUpdate, logQuery);
                combineMappings(queryUri, sparqlServer, commitUpdate, logQuery);
                addEmptyMapping(queryUri, sparqlServer, commitUpdate, logQuery);
            }
            startMapping(queryUri, sparqlServer, commitUpdate, logQuery);
            preventFrom(queryUri, sparqlServer, commitUpdate, logQuery);
//            commitUpdate = false;
            for (int j = 0; j < 12; j++) {
                makeProgress(queryUri, sparqlServer, commitUpdate, logQuery);
            }
            validateMappings(queryUri, sparqlServer, commitUpdate, logQuery);
        } while (!allPatternsAreMapped(queryUri, sparqlServer, false));
    }

    private void initializeSpCollectionMapping(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {

        // specify for all subpatterns that they have not yet been mapped to the current query
        String query = "# specify for all subpatterns that they have not yet been mapped to the current query\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?spc queries:isNotMappedToQuery <" + queryUri + ">.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?spc a patterns:SubpatternCollection.\n"
                + "  }\n"
                + "}";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }
    }

    private boolean allPatternsAreMapped(String queryUri, SparqlClient sparqlServer, boolean logQuery) {
        String query = "# are all patterns mapped to the current query?\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "ASK\n"
                + "{\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?p a patterns:Pattern.\n"
                + "  }\n"
                + "  FILTER NOT EXISTS\n"
                + "  {\n"
                + "    GRAPH <" + queriesNamedGraphUri + "> { ?p queries:toConsiderInMappingQuery <" + queryUri + ">. }\n"
                + "  }\n"
                + "}";

        if (logQuery) {
            logger.info(query);
        }
        boolean result = !sparqlServer.ask(query);
        logger.info("-> " + result);
        return result;
    }

    /**
     * 
     * @param queryUri
     * @param sparqlServer
     * @param commitUpdate 
     */
    private void removeMappingsOfEmptyMappings(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {

        String query = "# remove mappings of contingent subpattern collections containing only empty mappings\n"
                + "# an empty mapping for the contingent subpattern collection itself will be added later\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
                + "DELETE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?m ?a ?b.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?spc patterns:hasCardinalityMin \"0\"^^xsd:int.\n"
                + "  }\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?spc queries:toConsiderInMappingQuery <" + queryUri + ">.\n"
                + "    ?m queries:mappingHasQuery <" + queryUri + ">;\n"
                + "       queries:mappingHasPatternConstituent ?spc;\n"
                + "       ?a ?b.\n"
                + "    FILTER NOT EXISTS\n"
                + "    {\n"
                + "      ?m queries:mappingContainsMapping+ ?em.\n"
                + "      ?em queries:emHasScore ?score.\n"
                + "      FILTER (?score > 0)\n"
                + "    }\n"
                + "  }\n"
                + "}\n";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }
    }

    /**
     * combine mappings of repeatable subpattern collections
     * @param queryUri
     * @param sparqlServer
     * @param commitUpdate 
     */
    private void combineMappings(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {

        String query = "# combine mappings of repeatable subpattern collections\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?mappingUri a queries:SubpatternCollectionMapping;\n"
                + "                a queries:CombinationMapping;\n"
                + "                queries:mappingHasPatternConstituent ?spc;\n"
                + "                queries:mappingContainsMapping ?m1, ?m2;\n"
                + "                queries:mappingHasQuery <" + queryUri + ">.\n"
                + "    ?spc queries:allreadyCombinedForQuery <" + queryUri + ">.\n"
                //                + "    # process the descriptive sentence\n"
                //                + "    ?mappingUri queries:hasDescriptiveSubsentence ?descSubsent.\n"
                //                + "    ?descSubsent a queries:DescriptiveSubsentence;\n"
                //                + "                 queries:isMadeUpOfList ?list;\n"
                //                + "                 queries:isMadeUpOfList-for ?listFor1, ?listFor2.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "    SELECT DISTINCT ?spc WHERE\n"
                + "    {\n"
                + "      ?spc patterns:hasCardinalityMax \"2\"^^xsd:int.\n"
                + "    }\n"
                + "  }\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?spc queries:toConsiderInMappingQuery <" + queryUri + ">.\n"
                + "    FILTER NOT EXISTS\n"
                + "    {\n"
                + "      ?spc queries:allreadyCombinedForQuery <" + queryUri + ">.\n"
                + "    }\n"
                + "    ?m1 queries:mappingHasPatternConstituent ?spc.\n"
                + "    ?m2 queries:mappingHasPatternConstituent ?spc.\n"
                + "    FILTER ( STR(?m1) < STR(?m2) )\n"
                //                + "    # process the descriptive sentence\n"
                //                + "    ?m1 (queries:hasDescriptiveSubsentence/queries:isMadeUpOfList) ?list.\n"
                //                + "    ?m1 (queries:hasDescriptiveSubsentence/queries:isMadeUpOfList-for) ?listFor1.\n"
                //                + "    ?m2 (queries:hasDescriptiveSubsentence/queries:isMadeUpOfList-for) ?listFor2.\n"
                + "  }\n"
                + "  BIND (UUID() AS ?mappingUri)\n"
                + "  BIND (UUID() AS ?descSubsent)\n"
                + "}\n";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }
    }

    /**
     * add an empty mapping to ContingentSubpatternCollections to be considered in next mappings
     * @param queryUri
     * @param sparqlServer
     * @param commitUpdate 
     */
    private void addEmptyMapping(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {

        String query = "# add an empty mapping to ContingentSubpatternCollections to be considered in next mappings\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?mappingUri a queries:EmptySubpatternCollectionMapping;\n"
                + "                a queries:SubpatternCollectionMapping;\n" // we don't use any reasonner in queries graph
                + "                queries:mappingHasPatternConstituent ?spc;\n"
                + "                queries:mappingHasQuery <" + queryUri + ">;\n"
                //                + "                queries:hasDescriptiveSubsentence ?descSentUri.\n"
                //                + "    ?descSentUri a queries:DescriptiveSubsentence;\n"
                //                + "                   queries:hasStringValue \"(EmptySubpatternCollectionMapping)\".\n"
                + "  }\n"
                + "}\n"
                + "WHERE # select all ContingentSubpatternCollections which don't have yet an EmptySubpatternCollectionMapping\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?spc queries:toConsiderInMappingQuery <" + queryUri + ">.\n"
                + "    FILTER NOT EXISTS\n"
                + "    {\n"
                + "      ?mapping a queries:EmptySubpatternCollectionMapping;\n"
                + "                queries:mappingHasPatternConstituent ?spc;\n"
                + "                queries:mappingHasQuery <" + queryUri + ">.\n"
                + "    }\n"
                + "  }\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "    SELECT DISTINCT ?spc WHERE\n"
                + "    {\n"
                + "      ?spc a patterns:SubpatternCollection;\n"
                + "           patterns:hasCardinalityMin \"0\"^^xsd:int.\n"
                + "    }\n"
                + "  }\n"
                + "  BIND (UUID() AS ?mappingUri)\n"
                + "}";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }
    }

    /**
     * start mapping of SubpatternCollections whose all contained components are already mapped
     * @param queryUri
     * @param sparqlServer
     * @param commitUpdate 
     */
    private void startMapping(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {

        String query = "# start mapping of SubpatternCollections whose all contained components are already mapped\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "DELETE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?spc queries:isNotMappedToQuery <" + queryUri + ">.\n"
                + "  }\n"
                + "}\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?spc queries:isBeingMappedToQuery <" + queryUri + ">.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  {\n"
                + "    # get all SubpatternCollections which are not mapped but whose contained SubpatternCollections are 'to be considered'\n"
                + "    SELECT ?spc WHERE\n"
                + "    {\n"
                + "      # all SubpatternCollections\n"
                + "      GRAPH <" + patternsNamedGraphUri + ">\n"
                + "      {\n"
                + "        ?spc a patterns:SubpatternCollection.\n"
                + "      }\n"
                + "      # which are not mapped\n"
                + "      GRAPH <" + queriesNamedGraphUri + ">\n"
                + "      {\n"
                + "        ?spc queries:isNotMappedToQuery <" + queryUri + ">.\n"
                + "      }\n"
                + "      # whose contained SubpatternCollections are all 'to be considered'\n"
                + "      MINUS\n"
                + "      {\n"
                + "        # get all SubpatternCollections whose at least one contained SubpatternCollection is being mapped not mapped\n"
                + "        SELECT ?spc WHERE\n"
                + "        {\n"
                + "          GRAPH <" + patternsNamedGraphUri + ">\n"
                + "          {\n"
                + "            ?spc a patterns:SubpatternCollection;\n"
                + "                 patterns:hasDirectSubpattern ?spc2.\n"
                + "            ?spc2 a patterns:SubpatternCollection;\n"
                + "          }\n"
                + "          GRAPH <" + queriesNamedGraphUri + ">\n"
                + "          {\n"
                + "            ?spc2 (queries:isNotMappedToQuery | queries:isBeingMappedToQuery) <" + queryUri + ">.\n"
                + "          }\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}\n";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }
    }

    /**
     * prevent from ...
     * @param queryUri
     * @param sparqlServer
     * @param commitUpdate 
     */
    private void preventFrom(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {

        String query = "# prevent from adding in a subpattern collection mapping a element mapping\n"
                + "# relative to a pattern element appearing outside of this subpattern collection\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "DELETE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pc queries:toConsiderInMappingQuery <" + queryUri + ">.\n"
                + "  }\n"
                + "}\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pc queries:toConsiderNextStepInMappingQuery <" + queryUri + ">.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  SELECT DISTINCT ?pc WHERE\n"
                + "  {\n"
                + "    GRAPH <" + queriesNamedGraphUri + ">\n"
                + "    {\n"
                + "      ?spc queries:isBeingMappedToQuery <" + queryUri + ">.\n"
                + "      ?pc queries:toConsiderInMappingQuery <" + queryUri + ">.\n"
                + "    }\n"
                + "    GRAPH <" + patternsNamedGraphUri + ">\n"
                + "    {\n"
                + "      ?spc patterns:isMadeUpOf ?pc.\n"
                + "      OPTIONAL\n"
                + "      {\n"
                + "        ?spc2 patterns:isMadeUpOf ?pc.\n"
                + "        FILTER ( !sameTerm(?spc, ?spc2) )\n"
                + "      }\n"
                + "      FILTER NOT EXISTS\n"
                + "      {\n"
                + "        { ?spc patterns:isMadeUpOf ?spc2. }\n"
                + "        UNION\n"
                + "        { ?spc2 patterns:isMadeUpOf ?spc. }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}\n";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }
    }

    /**
     * make progress the mappings of currently processed SubpatternCollections
     * @param queryUri
     * @param sparqlServer
     * @param commitUpdate 
     */
    private void makeProgress(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {

        String query = "# make progress the mappings of currently processed SubpatternCollections\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "DELETE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pc queries:toConsiderInMappingQuery <" + queryUri + ">.\n"
                + "    ?pc queries:allreadyCombinedForQuery <" + queryUri + ">.\n"
                + "    # to not consider these mappings anymore\n"
                + "    ?mPc queries:mappingHasPatternConstituent ?pc.\n"
                + "    ?mSpc queries:mappingHasPatternConstituent ?spc.\n"
                + "  }\n"
                + "}\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pc queries:isMappedToQuery <" + queryUri + ">.\n"
                + "    ?mappingUri a queries:SubpatternCollectionMapping;\n"
                + "                queries:mappingHasPatternConstituent ?spc;\n"
                + "                queries:mappingContainsMapping ?mSpc, ?mPc;\n"
                + "                queries:mappingHasQuery <" + queryUri + ">.\n"
                + "    # used for descriptive sentence generation\n"
                + "    ?mPc queries:mappingHadPatternConstituent ?pc.\n"
                + "    ?mSpc queries:mappingHadPatternConstituent ?spc.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  {\n"
                + "    # select a pair (?spc, ?pc) such as\n"
                + "    #  - ?spc is a SubpatternCollection made of ?pc\n"
                + "    #  - ?spc is being mapped\n"
                + "    #  - ?pc is to be considered\n"
                + "    SELECT ?spc ?pc WHERE\n"
                + "    {\n"
                + "      GRAPH <" + queriesNamedGraphUri + ">\n"
                + "      {\n"
                + "        ?pc queries:toConsiderInMappingQuery <" + queryUri + ">.\n"
                + "        ?spc queries:isBeingMappedToQuery <" + queryUri + ">.\n"
                + "      }\n"
                + "      GRAPH <" + patternsNamedGraphUri + ">\n"
                + "      {\n"
                + "        ?spc patterns:isMadeUpOf ?pc.\n"
                + "      }\n"
                + "    } ORDER BY ?spc ?pc LIMIT 1\n"
                + "  }\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    OPTIONAL\n"
                + "    {\n"
                + "      ?mSpc queries:mappingHasPatternConstituent ?spc;\n"
                + "            queries:mappingHasQuery <" + queryUri + ">.\n"
                + "    }\n"
                + "    ?mPc queries:mappingHasPatternConstituent ?pc;\n"
                + "         queries:mappingHasQuery <" + queryUri + ">.\n"
                + "  }\n"
                + "  BIND (UUID() AS ?mappingUri)\n"
                + "}\n";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }
    }

    /**
     * validate mappings of SubpatternCollections whose...
     * @param queryUri
     * @param sparqlServer
     * @param commitUpdate 
     */
    private void validateMappings(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {

        String query = "# validate mappings of SubpatternCollections which are being mapped and whose contained SubpatternCollections are all mapped or to be mapped in next step\n"
                + "# change the status of toConsiderNextStepInMappingQuery SubpatternCollections into toConsiderInMappingQuery\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "DELETE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?spc queries:isBeingMappedToQuery <" + queryUri + ">.\n"
                + "    ?comp queries:toConsiderNextStepInMappingQuery <" + queryUri + ">.\n"
                + "  }\n"
                + "}\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?spc queries:toConsiderInMappingQuery <" + queryUri + ">.\n"
                + "    ?comp queries:toConsiderInMappingQuery <" + queryUri + ">.\n"
                //                + "    # process the descriptive sentence\n"
                //                + "    ?spcm queries:hasDescriptiveSubsentence ?descSubsent.\n"
                //                + "    ?descSubsent a queries:DescriptiveSubsentence;\n"
                //                + "                 ?madeUpRelation ?listSerialized .\n"
                //                + "    ?oneListSerialized rdf:first ?oneElementSerializedSsit ;\n"
                //                + "                       rdf:first ?oneElementSerializedFlit ;\n"
                //                + "                       rdf:first ?oneElementSerializedOther ;\n"
                //                + "                       rdf:firsttt ?plop ;\n"
                //                + "                       rdf:rest ?nextListSerialized .\n"
                //                + "    ?oneElementSerializedSsit queries:hasStringValue ?ssitValue.\n"
                //                + "    ?oneElementSerializedFlit queries:insertForSubsentenceOf ?spc.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  {\n"
                + "    # get all SubpatternCollections which are being mapped and whose contained SubpatternCollections are all mapped or to be mapped in next step\n"
                + "    SELECT ?spc WHERE\n"
                + "    {\n"
                + "      # all SubpatternCollections\n"
                + "      GRAPH <" + patternsNamedGraphUri + ">\n"
                + "      {\n"
                + "        ?spc a patterns:SubpatternCollection.\n"
                + "      }\n"
                + "      # which are being mapped\n"
                + "      GRAPH <" + queriesNamedGraphUri + ">\n"
                + "      {\n"
                + "        ?spc queries:isBeingMappedToQuery <" + queryUri + ">.\n"
                + "      }\n"
                + "      # whose contained SubpatternCollections are all mapped or to be mapped in next step\n"
                + "      MINUS\n"
                + "      {\n"
                + "        # get all SubpatternCollections whose at least one contained component is not mapped\n"
                + "        SELECT DISTINCT ?spc WHERE\n"
                + "        {\n"
                + "          GRAPH <" + patternsNamedGraphUri + ">\n"
                + "          {\n"
                + "            ?spc a patterns:SubpatternCollection;\n"
                + "                 patterns:isMadeUpOf ?comp.\n"
                + "          }\n"
                + "          GRAPH <" + queriesNamedGraphUri + ">\n"
                + "          {\n"
                + "            ?comp queries:toConsiderInMappingQuery <" + queryUri + ">.\n"
                + "          }\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "  # change the status of toConsiderNextStepInMappingQuery SubpatternCollections into toConsiderInMappingQuery\n"
                + "  OPTIONAL\n"
                + "  {\n"
                + "    GRAPH <" + patternsNamedGraphUri + ">\n"
                + "    {\n"
                + "      ?spc patterns:isMadeUpOf ?comp.\n"
                + "    }\n"
                + "    GRAPH <" + queriesNamedGraphUri + ">\n"
                + "    {\n"
                + "      ?comp queries:toConsiderNextStepInMappingQuery <" + queryUri + ">.\n"
                + "    }\n"
                + "  }\n"
                //                + "  # process the descriptive sentence\n"
                //                + "  GRAPH <" + queriesUri + ">\n"
                //                + "  {\n"
                //                + "    ?spcm queries:mappingHasPatternConstituent ?spc.\n"
                //                + "  }\n"
                //                + "  GRAPH <" + patternsUri + ">\n"
                //                + "  {\n"
                //                + "    ?sst (patterns:sstTargetsPc|^patterns:hasSentenceTemplate) ?spc ;\n"
                //                + "         patterns:isMadeUpOfList ?list ;\n"
                //                
                //                
                //                + "    # when dealing with spc containing a for loop, two subsentences are generated (one for the inner for loop, one for the whole spc) \n"
                //                + "    OPTIONAL {\n"
                //                + "      ?sst a patterns:ForLoopInTemplate.\n"
                //                + "      BIND (queries:isMadeUpOfList-for AS ?madeUpRelation).\n"
                //                + "    }\n"
                //                + "    OPTIONAL {\n"
                //                + "      ?sst a patterns:SpcInTemplate.\n"
                //                + "      BIND (queries:isMadeUpOfList AS ?madeUpRelation).\n"
                //                + "    }\n"
                //                
                //                
                //                + "    ?list rdf:rest* ?oneList .\n"
                //                + "    ?oneList rdf:first ?oneElement ;\n"
                //                + "             rdf:rest ?nextList .\n"
                //                + "  }\n"
                //                + "  # oneElement is a static string\n"
                //                + "  OPTIONAL\n"
                //                + "  {\n"
                //                + "    GRAPH <" + patternsUri + "> { ?oneElement patterns:ssitHasValue ?ssitValue. \n"
                //                + "                           BIND (UUID() AS ?oneElementSerializedSsit) }\n"
                //                + "  }\n"
                //                + "  # oneElement is a ForLoopInTemplate\n"
                //                + "  OPTIONAL\n"
                //                + "  {\n"
                //                + "    GRAPH <" + patternsUri + "> { ?oneElement a patterns:ForLoopInTemplate. \n"
                //                + "                           BIND (UUID() AS ?oneElementSerializedFlit) }\n"
                //                + "  }\n"
                //                + "  # oneElement is a PeInTemplate or a SpcInTemplate\n"
                //                + "  OPTIONAL\n"
                //                + "  {\n"
                //                + "    GRAPH <" + patternsUri + "> { {?oneElement a patterns:PeInTemplate.} UNION {?oneElement a patterns:SpcInTemplate.}\n"
                //                + "                           ?oneElement patterns:sstTargetsPc ?pc. }\n"
                //                + "    GRAPH <" + queriesUri + "> { ?spcm queries:mappingContainsMapping+ ?sspcm. \n"
                //                + "                          ?sspcm queries:mappingHadPatternConstituent ?pc; \n"
                //                + "                                 queries:hasDescriptiveSubsentence ?oneElementSerializedOther.\n"
                //                + "                          FILTER NOT EXISTS # because of combined mappings which maps same spc as their contained mappings \n"
                //                + "                          # FIXME: this part makes the query very slow (for something that looks simple) \n"
                //                + "                          {\n"
                //                + "                            ?spcm queries:mappingContainsMapping+ ?sspcm2.\n"
                //                + "                            ?sspcm2 queries:mappingHadPatternConstituent ?pc;\n"
                //                + "                                    queries:mappingContainsMapping ?sspcm.\n"
                //                + "                          }}\n"
                //                + "    BIND (<SpcInTemplate> AS ?plop)\n"
                //                + "  }\n"
                //                + "  BIND (IRI(CONCAT(str(?spcm), \"_descSubsent\")) AS ?descSubsent)\n"
                //                + "  BIND (IRI(CONCAT(str(?spcm), str(?list))) AS ?listSerialized)\n"
                //                + "  BIND (IRI(CONCAT(str(?spcm), str(?oneList))) AS ?oneListSerialized)\n"
                //                + "  BIND (IRI(CONCAT(str(?spcm), str(?nextList))) AS ?nextListSerialized)\n"
                + "}\n";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }
    }

    /**
     * 
     * @param queryUri
     * @param sparqlServer
     * @param commitUpdate 
     */
    private void performPatternMapping(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {
        // change query processing state
        Controller.getInstance().changeQueryProcessingState(queryUri, sparqlServer, queriesNamedGraphUri, "PerformingPatternMapping");

        String query = "# perform pattern mapping\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "DELETE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?p queries:toConsiderInMappingQuery <" + queryUri + ">.\n"
                + "  }\n"
                + "}\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?p queries:isMappedToQuery <" + queryUri + ">.\n"
                + "    ?pm a queries:PatternMapping.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm queries:mappingHasPatternConstituent ?p;\n"
                + "        queries:mappingHasQuery <" + queryUri + ">.\n"
                + "    FILTER NOT EXISTS {?pm2 queries:mappingContainsMapping ?pm.}\n"
                + "  }\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?p a patterns:Pattern.\n"
                + "  }\n"
                + "}\n";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }
    }

    private void performMappingRanking(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {
        // change query processing state
        Controller.getInstance().changeQueryProcessingState(queryUri, sparqlServer, queriesNamedGraphUri, "PerformingMappingRanking");

        String query = "# process the element mapping relevance mark\n"
                + "# step 1: process the average score of involved element mappings\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm queries:hasEmAvg ?avgscore;\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  SELECT ?pm (AVG(?score) AS ?avgscore) WHERE\n"
                + "    {\n"
                + "    GRAPH <" + queriesNamedGraphUri + ">\n"
                + "    {\n"
                + "      ?pm a queries:PatternMapping;\n"
                + "          queries:mappingHasQuery <" + queryUri + ">;\n"
                + "          queries:mappingContainsMapping+ ?em.\n"
                + "      ?em queries:emHasScore ?score.\n"
                + "    }\n"
                + "  } GROUP BY ?pm\n"
                + "};\n"
                + "# step 2: disavantage query mappings involving several times a same keyword\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm queries:hasEmFactor ?factor;\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  SELECT ?pm (COUNT(distinct ?kw) / COUNT(distinct ?em) AS ?factor ) WHERE\n"
                + "  {\n"
                + "    GRAPH <" + queriesNamedGraphUri + ">\n"
                + "    {\n"
                + "      ?pm a queries:PatternMapping;\n"
                + "          queries:mappingHasQuery <" + queryUri + ">;\n"
                + "          queries:mappingContainsMapping+ ?em.\n"
                + "      FILTER NOT EXISTS {?em a queries:EmptyElementMapping.}\n"
                + "      ?em (queries:emHasMatching / queries:matchingHasKeyword) ?kw.\n"
                + "    }\n"
                + "  } GROUP BY ?pm\n"
                + "};\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm queries:hasEmrMark ?emrmark;\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm queries:hasEmAvg ?avgscore.\n"
                + "    ?pm queries:hasEmFactor ?factor.\n"
                + "    BIND (?factor * ?avgscore AS ?emrmark)\n"
                + "  }\n"
                + "}\n";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }

        query = "# process the query coverage relevance mark\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm queries:hasQcrMark ?qcrmark;\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  {\n"
                + "    SELECT (COUNT(?qe) AS ?nbqe) WHERE\n"
                + "    {\n"
                + "      GRAPH <" + queriesNamedGraphUri + ">\n"
                + "      {\n"
                + "        <" + queryUri + "> queries:queryHasQueryElement ?qe.\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "  {\n"
                + "    SELECT ?pm (COUNT(DISTINCT ?qe) AS ?nbmappedqe) WHERE\n"
                + "    {\n"
                + "      GRAPH <" + queriesNamedGraphUri + ">\n"
                + "      {\n"
                + "        ?pm a queries:PatternMapping;\n"
                + "            queries:mappingHasQuery <" + queryUri + ">;\n"
                //                + "            (queries:mappingContainsMapping+/queries:hasInstanceMapping*/queries:hasClassMapping*) ?em.\n"
                //                + "        ?em (queries:emHasMatching / queries:matchingHasKeyword) ?qe.\n"
                + "            queries:mappingContainsMapping+ ?em.\n"
                + "        ?em queries:emHasQueryElement ?qe.\n"
                + "      }\n"
                + "    } GROUP BY ?pm\n"
                + "  }\n"
                + "  BIND ( ?nbmappedqe / ?nbqe AS ?qcrmark )\n"
                + "}\n";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }

        query = "# process the pattern coverage relevance mark\n"
                + "# in two step probably because of a bug in ARQ\n"
                + "# step 1: find out for each query mapping the number of element mapping with distinct pattern element\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm queries:numberOfSignificantElementMappings ?nbmappedpe;\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  select ?pm (count(distinct ?pe) as ?nbmappedpe) where\n"
                + "  {\n"
                + "      GRAPH <" + queriesNamedGraphUri + ">\n"
                + "      {\n"
                + "        ?pm a queries:PatternMapping;\n"
                + "            queries:mappingHasQuery <" + queryUri + ">;\n"
                + "            queries:mappingHasPatternConstituent ?p;\n"
                + "            queries:mappingContainsMapping+ ?em.\n"
                + "        ?em queries:emHasPatternElement ?pe.\n"
                + "        FILTER NOT EXISTS { ?em a queries:EmptyElementMapping. }\n"
                + "      }\n"
                + "  } group by ?pm\n"
                + "};\n"
                + "# step 2: find out for each query mapping the total number of element mappings\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm queries:numberOfElementMappings ?nbmappedpe;\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  select ?pm (count(distinct ?pe) as ?nbmappedpe) where\n"
                + "  {\n"
                + "      GRAPH <" + queriesNamedGraphUri + ">\n"
                + "      {\n"
                + "        ?pm a queries:PatternMapping;\n"
                + "            queries:mappingHasQuery <" + queryUri + ">;\n"
                + "            queries:mappingHasPatternConstituent ?p;\n"
                + "            queries:mappingContainsMapping+ ?em.\n"
                + "        ?em queries:emHasPatternElement ?pe.\n"
                + "      }\n"
                + "  } group by ?pm\n"
                + "};\n"
                + "# step 3: \n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm queries:hasPcrMark ?pcrmark;\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm queries:mappingHasQuery <" + queryUri + ">;\n"
                + "        queries:numberOfElementMappings ?nbem;\n"
                + "        queries:numberOfSignificantElementMappings ?nbsem.\n"
                + "  }\n"
                + "  BIND (?nbsem / ?nbem AS ?pcrmark)\n"
                + "}";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }

        query = "# process final relevance mark\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm queries:hasRelevanceMark ?rmark;\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm a queries:PatternMapping;\n"
                + "        queries:mappingHasQuery <" + queryUri + ">;\n"
                + "        queries:hasEmrMark ?emrmark;\n"
                + "        queries:hasQcrMark ?qcrmark;\n"
                + "        queries:hasPcrMark ?pcrmark.\n"
                + "  }\n"
                + "  BIND ( ?emrmark * ?qcrmark * ?pcrmark AS ?rmark )\n"
                //                + "  BIND ( ?qcrmark * ?pcrmark AS ?rmark )\n"
                + "}\n";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }


    }

    /**
     * TODO: check that everything that must be cleared is actually cleared
     * @param queryUri
     * @param sparqlServer
     * @param commitUpdate 
     */
    private void clearMappings(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {
        // change query processing state
        Controller.getInstance().changeQueryProcessingState(queryUri, sparqlServer, queriesNamedGraphUri, "PerformingKbClearing");

        // in two steps for efficiency reasons

        // step one
        String query = "# clear all mappings whose rank is over a given threshold\n"
                + "# step 1: clear bad query mappings\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "DELETE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm ?c ?d .\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    {\n"
                + "      SELECT * WHERE \n"
                + "      {\n"
                + "        ?pm queries:mappingHasQuery <" + queryUri + ">;\n"
                + "            queries:hasRelevanceMark ?rmark.\n"
                + "        \n"
                + "      } ORDER BY DESC(?rmark) ?pm OFFSET " + this.numMappings + "\n"
                + "    }\n"
                + "    ?pm ?c ?d .\n"
                //                + "    OPTIONAL { # optional is temporary\n"
                //                + "    ?pm queries:hasDescriptiveSubsentence ?descSubsent .\n"
                //                + "    ?descSubsent a queries:DescriptiveSubsentence ;\n"
                //                + "                 queries:isMadeUpOfList ?list .\n"
                //                + "    ?list rdf:rest* ?rest .\n"
                //                + "    ?rest rdf:rest ?rest2 ;\n"
                //                + "          rdf:first ?first .\n"
                //                + "    ?first ?e ?f .\n"
                //                + "    }\n"
                + "  }\n"
                + "};\n"
                + "# step 2: clear spc mappings which are not contained in any query mapping\n"
                + "DELETE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?spcm ?a ?b .\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?spcm a queries:SubpatternCollectionMapping .\n"
                //                + "    FILTER NOT EXISTS { ?spcm a queries:PatternMapping. }\n"
                + "    FILTER NOT EXISTS { ?pm a queries:PatternMapping; \n"
                + "                            queries:mappingContainsMapping* ?spcm. }\n"
                + "    ?spcm ?a ?b .\n"
                + "  }\n"
                + "}\n";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }
    }

    private void generateDescriptiveSentences(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {
        // change query processing state
        Controller.getInstance().changeQueryProcessingState(queryUri, sparqlServer, queriesNamedGraphUri, "PerformingSentenceGeneration");

        String query = "# generate descriptive sentences\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "# step 1: add an empty descriptive sentence to each subpattern collection\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "     ?spcm queries:hasDescriptiveSubsentence ?descSubsent.\n"
                + "     ?descSubsent a queries:DescriptiveSubsentence;\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?spcm a queries:SubpatternCollectionMapping ;\n"
                + "          queries:mappingHasQuery <" + queryUri + "> ;\n"
                + "    FILTER NOT EXISTS { ?spcm a queries:EmptySubpatternCollectionMapping . }\n"
                + "    BIND (IRI(CONCAT(str(?spcm), \"_descSubsent\")) AS ?descSubsent) \n"
                + "  }\n"
                + "};\n"
                + "# step 2: build for each spc a list linking to children subsentences\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?descSubsent ?madeUpRelation ?listSerialized .\n"
                + "    ?oneListSerialized rdf:first ?oneElementSerializedSsit ;\n"
                + "                       rdf:first ?oneElementSerializedFlit ;\n"
                + "                       rdf:first ?oneElementSerializedOther ;\n"
                + "                       rdf:rest ?nextListSerialized .\n"
                + "    ?oneElementSerializedSsit queries:hasStringValue ?ssitValue.\n"
                + "    ?oneElementSerializedFlit queries:insertForSubsentenceOf ?spc.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?spcm queries:hasDescriptiveSubsentence ?descSubsent;\n"
                + "          (queries:mappingHasPatternConstituent|queries:mappingHadPatternConstituent) ?spc.\n"
                + "  }\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?sst (patterns:sstTargetsPc|^patterns:hasSentenceTemplate) ?spc ;\n"
                + "         patterns:isMadeUpOfList ?list ;\n"
                + "    # when dealing with spc containing a for loop, two subsentences are generated (one for the inner for loop, one for the whole spc) \n"
                + "    OPTIONAL {\n"
                + "      ?sst a patterns:ForLoopInTemplate.\n"
                + "      BIND (queries:isMadeUpOfList-for AS ?madeUpRelation).\n"
                + "    }\n"
                + "    OPTIONAL {\n"
                + "      ?sst a patterns:SpcInTemplate.\n"
                + "      BIND (queries:isMadeUpOfList AS ?madeUpRelation).\n"
                + "    }\n"
                + "    ?list rdf:rest* ?oneList .\n"
                + "    ?oneList rdf:first ?oneElement ;\n"
                + "             rdf:rest ?nextList .\n"
                + "  }\n"
                + "  # oneElement is a static string\n"
                + "  OPTIONAL\n"
                + "  {\n"
                + "    GRAPH <" + patternsNamedGraphUri + "> { ?oneElement patterns:ssitHasValue ?ssitValue. \n"
                + "                           BIND (UUID() AS ?oneElementSerializedSsit) }\n"
                + "  }\n"
                + "  # oneElement is a ForLoopInTemplate\n"
                + "  OPTIONAL\n"
                + "  {\n"
                + "    GRAPH <" + patternsNamedGraphUri + "> { ?oneElement a patterns:ForLoopInTemplate. \n"
                + "                           BIND (UUID() AS ?oneElementSerializedFlit) }\n"
                + "  }\n"
                + "  # oneElement is a PeInTemplate or a SpcInTemplate\n"
                + "  OPTIONAL\n"
                + "  {\n"
                + "    GRAPH <" + patternsNamedGraphUri + "> { {?oneElement a patterns:PeInTemplate.} UNION {?oneElement a patterns:SpcInTemplate.}\n"
                + "                           ?oneElement patterns:sstTargetsPc ?pc. }\n"
                + "    GRAPH <" + queriesNamedGraphUri + "> { ?spcm queries:mappingContainsMapping+ ?sspcm. \n"
                + "                          ?sspcm queries:mappingHadPatternConstituent ?pc; \n"
                + "                                 queries:hasDescriptiveSubsentence ?oneElementSerializedOther.\n"
                + "                          FILTER NOT EXISTS # because of combined mappings which maps same spc as their contained mappings \n"
                + "                          # FIXME: this part makes the query very slow (for something that looks simple) \n"
                + "                          {\n"
                + "                            ?spcm queries:mappingContainsMapping+ ?sspcm2.\n"
                + "                            ?sspcm2 queries:mappingHadPatternConstituent ?pc;\n"
                + "                                    queries:mappingContainsMapping ?sspcm.\n"
                + "                          }}\n"
                + "  }\n"
                //                + "  BIND (IRI(CONCAT(str(?spcm), \"_descSubsent\")) AS ?descSubsent)\n"
                + "  BIND (IRI(CONCAT(str(?spcm), str(?list))) AS ?listSerialized)\n"
                + "  BIND (IRI(CONCAT(str(?spcm), str(?oneList))) AS ?oneListSerialized)\n"
                + "  BIND (IRI(CONCAT(str(?spcm), str(?nextList))) AS ?nextListSerialized)\n"
                //                + "};\n"        
                //        
                //                + "# step 3: handle the case of mappings issued from combinations\n"
                //                + "INSERT\n"
                //                + "{\n"
                //                + "  GRAPH <" + queriesUri + ">\n"
                //                + "  {\n"
                //                + "    ?descSubsent a queries:DescriptiveSubsentence;\n"
                //                + "                 queries:isMadeUpOfList ?list;\n"
                //                + "                 queries:isMadeUpOfList-for ?listFor1, ?listFor2.\n"
                //                + "  }\n"
                //                + "}\n"
                //                + "WHERE\n"
                //                + "{\n"
                //                + "  GRAPH <" + queriesUri + ">\n"
                //                + "  {\n"
                //                + "    ?m a queries:CombinationMapping;\n"
                //                + "       queries:mappingContainsMapping ?m1, ?m2;\n"
                //                + "       queries:hasDescriptiveSubsentence ?descSubsent.\n"
                //                + "    ?m1 (queries:hasDescriptiveSubsentence/queries:isMadeUpOfList) ?list.\n"
                //                + "    ?m1 (queries:hasDescriptiveSubsentence/queries:isMadeUpOfList-for) ?listFor1.\n"
                //                + "    ?m2 (queries:hasDescriptiveSubsentence/queries:isMadeUpOfList-for) ?listFor2.\n"
                //                + "  }\n"
                + "}\n";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }
    }

    private void generateSparqlQueries(String queryUri, SparqlClient sparqlServer, boolean commitUpdate, boolean logQuery) {
        // change query processing state
        Controller.getInstance().changeQueryProcessingState(queryUri, sparqlServer, queriesNamedGraphUri, "PerformingSparqlQueryGeneration");

//        commitUpdate = false;

        String query = "# generate SPARQL queries\n"
                + "PREFIX patterns:   <http://swip.univ-tlse2.fr/ontologies/Patterns#>\n"
                + "PREFIX queries:   <http://swip.univ-tlse2.fr/ontologies/Queries#>\n"
                + "prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "prefix sp:  <http://spinrdf.org/sp>\n"
                + "# step 1: sparql representation and implied triples of each element mapping\n"
                + "# step 1.1: keywords whom mapped KB element is an instance of the ressource targeted by pattern element\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "     ?em queries:hasSparqlRepresentation ?rep.\n"
                + "     ?pm queries:hasSparqlQuery ?sq.\n"
                + "     ?sq queries:hasTriple ?tripleUri1.\n"
                + "     ?tripleUri1 sp:subject    ?rep ;\n"
                + "                sp:predicate rdfs:label ;\n"
                + "                sp:object   ?l .\n"
                + "     ?sq queries:hasTriple ?tripleUri2.\n"
                + "     ?tripleUri2 sp:subject    ?rep ;\n"
                + "                    sp:predicate rdf:type ;\n"
                + "                    sp:object   ?targetedClass .\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm a queries:PatternMapping ;\n"
                + "        queries:mappingHasQuery <" + queryUri + "> ;\n"
                + "        queries:mappingContainsMapping+ ?em .\n"
                + "    ?em queries:emHasPatternElement ?pe;\n"
                + "        queries:emHasKeyword ?keyword;\n"
                + "        queries:emHasMatchedLabel ?l;\n"
                + "        queries:emHasResource ?r.\n"
                + "    ?keyword queries:queryElementHasValue ?kwValue.\n"
                + "  }\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "      ?pe patterns:targetsClass ?targetedClass.\n"
                + "  }\n"
                + "  GRAPH <" + kbLocation + ">\n"
                + "  {\n"
                + "      ?r a ?c.\n"
                + "      ?c rdfs:subClassOf* ?targetedClass.\n"
                + "  }\n"
                + "  BIND (IRI(CONCAT(str(?pm), \"_sparql\")) AS ?sq) \n"
                + "  BIND (IRI(CONCAT(str(?em), \"_impliedTriple1\")) AS ?tripleUri1)\n"
                + "  BIND (IRI(CONCAT(str(?em), \"_impliedTriple2\")) AS ?tripleUri2)\n"
                + "  BIND (CONCAT (\"?\", REPLACE(?kwValue, \" \", \"_\", \"i\")) AS ?rep)\n"
                + "};\n"
                + "# step 1.2: keywords whom mapped KB element is a subclass of the ressource targeted by pattern element\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "     ?em queries:hasSparqlRepresentation ?rep.\n"
                + "     ?pm queries:hasSparqlQuery ?sq.\n"
                + "     ?sq queries:hasTriple ?tripleUri1.\n"
                + "     ?sq queries:hasTriple ?tripleUri2.\n"
                + "     ?tripleUri2 sp:subject    ?rep ;\n"
                + "                    sp:predicate rdf:type ;\n"
                + "                    sp:object   ?r .\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm a queries:PatternMapping ;\n"
                + "        queries:mappingHasQuery <" + queryUri + "> ;\n"
                + "        queries:mappingContainsMapping+ ?em .\n"
                + "    ?em queries:emHasPatternElement ?pe;\n"
                + "        queries:emHasKeyword ?keyword;\n"
                + "        queries:emHasResource ?r.\n"
                + "    ?keyword queries:queryElementHasValue ?kwValue.\n"
                + "  }\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "      ?pe patterns:targetsClass ?targetedClass.\n"
                + "  }\n"
                + "  GRAPH <" + kbLocation + ">\n"
                + "  {\n"
                + "      ?r rdfs:subClassOf* ?targetedClass.\n"
                + "  }\n"
                + "  BIND (IRI(CONCAT(str(?pm), \"_sparql\")) AS ?sq) \n"
                + "  BIND (IRI(CONCAT(str(?em), \"_impliedTriple1\")) AS ?tripleUri1)\n"
                + "  BIND (IRI(CONCAT(str(?em), \"_impliedTriple2\")) AS ?tripleUri2)\n"
                + "  BIND (CONCAT (\"?\", REPLACE(?kwValue, \" \", \"_\", \"i\")) AS ?rep)\n"
                + "};\n"
                + "# step 1.3: keywords whom mapped KB element is a property\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "     ?em queries:hasSparqlRepresentation ?r.\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm a queries:PatternMapping ;\n"
                + "        queries:mappingHasQuery <" + queryUri + "> ;\n"
                + "        queries:mappingContainsMapping+ ?em .\n"
                + "    ?em queries:emHasPatternElement ?pe;\n"
                + "        queries:emHasResource ?r;\n"
                + "        queries:emHasKeyword ?keyword.\n"
                + "    ?keyword queries:queryElementHasValue ?kwValue.\n"
                + "  }\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "      ?pe patterns:targetsProperty ?targetedProp.\n"
                + "  }\n"
                + "  BIND (IRI(CONCAT(str(?pm), \"_sparql\")) AS ?sq) \n"
                + "};\n"
                + "# step 1.4: literals\n"
                + "# TODO\n"
                + "# step 2:\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "     ?pm queries:hasSparqlQuery ?sq.\n"
                //                + "     ?sq a sp:Select _:list.\n"
                + "     ?sq queries:hasTriple ?tripleUri."
                + "     ?tripleUri sp:subject   ?repS ;\n"
                + "                sp:predicate ?repP ;\n"
                + "                sp:object    ?repO .\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?p patterns:patternHasSubpattern ?pt .\n"
                + "    ?pt patterns:hasSubject ?subj ;\n"
                + "        patterns:hasProperty ?prop ;\n"
                + "        patterns:hasObject ?obj .\n"
                + "  }\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "    ?pm a queries:PatternMapping ;\n"
                + "        queries:mappingHasPatternConstituent ?p ;\n"
                + "        queries:mappingHasQuery <" + queryUri + "> ;\n"
                + "        queries:mappingContainsMapping+ ?emS, ?emP, ?emO .\n"
                + "    ?emS queries:emHasPatternElement ?subj .\n"
                + "    ?emP queries:emHasPatternElement ?prop .\n"
                + "    ?emO queries:emHasPatternElement ?obj .\n"
                + "    ?emS queries:hasSparqlRepresentation ?repS.\n"
                + "    ?emP queries:hasSparqlRepresentation ?repP.\n"
                + "    ?emO queries:hasSparqlRepresentation ?repO.\n"
                + "  }\n"
                + "  BIND (IRI(CONCAT(str(?pm), \"_sparql\")) AS ?sq) \n"
                + "  BIND (UUID() AS ?tripleUri)\n"
                + "};\n"
                + "# step 3: select variable (projection atribute)\n"
                + "INSERT\n"
                + "{\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "     ?sq queries:hasSelectVar <pliplo> .\n"
                + "  }\n"
                + "}\n"
                + "WHERE\n"
                + "{\n"
                + "  GRAPH <" + patternsNamedGraphUri + ">\n"
                + "  {\n"
                + "  }\n"
                + "  GRAPH <" + queriesNamedGraphUri + ">\n"
                + "  {\n"
                + "  }\n"
                + "}\n";

        if (logQuery) {
            logger.info(query);
        }
        if (commitUpdate) {
            sparqlServer.update(query);
        }
    }
}