package org.swip.pivotToMappings.model.patterns.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.swip.pivotToMappings.controller.Controller;
import org.swip.pivotToMappings.model.patterns.Pattern;
import org.swip.pivotToMappings.model.patterns.patternElement.PatternElement;
import org.swip.pivotToMappings.model.patterns.patternElement.PropertyPatternElement;
import org.swip.pivotToMappings.model.patterns.subpattern.PatternTriple;
import org.swip.pivotToMappings.model.patterns.subpattern.Subpattern;
import org.swip.pivotToMappings.model.patterns.subpattern.SubpatternCollection;
import org.swip.pivotToMappings.model.query.Query;
import org.swip.pivotToMappings.model.query.queryElement.QueryElement;
import org.swip.pivotToMappings.sparql.SparqlServer;

// bean! (mais pas les classes utilisees)
public class PatternToQueryMapping {

    private Pattern pattern = null;
    private List<ElementMapping> elementMappings = new LinkedList<ElementMapping>();
    // element mappings relevance mark
    private float rMap = -1;
    // query coverage relevance mark
    private float rQueryCov = -1;
    // pattern coverage relevance mark
    private float rPatternCov = -1;
    // respect of the number of queried elements relevance mark
    private float rNumQueried = -1;
    // final relevance mark
    private float relevanceMark = -1;
    private String sentence = null;
    private String sparqlQuery = null;
    // string description (result of toString) stored in order to display it in client application
    private String stringDescription = null;

    public PatternToQueryMapping() {
    }

    public PatternToQueryMapping(Pattern p) {
        this.pattern = p;
    }

    public PatternToQueryMapping(Pattern p, List<ElementMapping> ems) {
        this.pattern = p;
        this.elementMappings = ems;
    }

    /**
     * @return the pattern
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * @param pattern the pattern to set
     */
    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * @return the elementMappings
     */
    public List<ElementMapping> getElementMappings() {
        return elementMappings;
    }

    /**
     * @param elementMappings the elementMappings to set
     */
    public void setElementMappings(List<ElementMapping> elementMappings) {
        this.elementMappings = elementMappings;
    }

    /**
     * @return the rMap
     */
    public float getrMap() {
        return rMap;
    }

    public float getrMap(Query userQuery) {
        if (rMap < 0) {
            evaluateRelevanceMark(userQuery);
        }
        return rMap;
    }

    /**
     * @param rMap the rMap to set
     */
    public void setrMap(float rMap) {
        this.rMap = rMap;
    }

    /**
     * @return the rQueryCov
     */
    public float getrQueryCov() {
        return rQueryCov;
    }

    public float getrQueryCov(Query userQuery) {
        if (rQueryCov < 0) {
            evaluateRelevanceMark(userQuery);
        }
        return rQueryCov;
    }

    /**
     * @param rQueryCov the rQueryCov to set
     */
    public void setrQueryCov(float rQueryCov) {
        this.rQueryCov = rQueryCov;
    }

    /**
     * @return the rPatternCov
     */
    public float getrPatternCov() {
        return rPatternCov;
    }

    public float getrPatternCov(Query userQuery) {
        if (rPatternCov < 0) {
            evaluateRelevanceMark(userQuery);
        }
        return rPatternCov;
    }

    /**
     * @param rPatternCov the rPatternCov to set
     */
    public void setrPatternCov(float rPatternCov) {
        this.rPatternCov = rPatternCov;
    }

    /**
     * @return the rNumQueried
     */
    public float getrNumQueried() {
        return rNumQueried;
    }

    public float getrNumQueried(Query userQuery) {
        if (rNumQueried < 0) {
            evaluateRelevanceMark(userQuery);
        }
        return rNumQueried;
    }

    /**
     * @param rNumQueried the rNumQueried to set
     */
    public void setrNumQueried(float rNumQueried) {
        this.rNumQueried = rNumQueried;
    }

    /**
     * @return the relevanceMark
     */
    public float getRelevanceMark() {
        return relevanceMark;
    }

    public float getRelevanceMark(Query userQuery) {
        if (relevanceMark < 0) {
            evaluateRelevanceMark(userQuery);
        }
        return relevanceMark;
    }

    /**
     * @param relevanceMark the relevanceMark to set
     */
    public void setRelevanceMark(float relevanceMark) {
        this.relevanceMark = relevanceMark;
    }

    /**
     * @return the sentence
     */
    public String getSentence() {
        return sentence;
    }

    public String getSentence(SparqlServer server, Query userQuery) {
        if (this.sentence == null) {
            generateSentence(server, userQuery);
        }
        return sentence;
    }

    /**
     * @param sentence the sentence to set
     */
    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    /**
     * @return the sparqlQuery
     */
    public String getSparqlQuery() {
        return sparqlQuery;
    }

    public String getSparqlQuery(SparqlServer sparqlServer, Query userQuery) {
        if (this.sparqlQuery == null) {
            generateSparqlQuery(sparqlServer, userQuery);
        }
        return this.sparqlQuery;
    }

    /**
     * @param sparqlQuery the sparqlQuery to set
     */
    public void setSparqlQuery(String sparqlQuery) {
        this.sparqlQuery = sparqlQuery;
    }

    public String getStringDescription() {
        return stringDescription;
    }

    public void setStringDescription(String stringDescription) {
        this.stringDescription = stringDescription;
    }
    
    public void addElementMapping(ElementMapping elementMapping) {
        this.getElementMappings().add(elementMapping);
    }

    public void addElementMappings(List<ElementMapping> elementMappings) {
        for (ElementMapping em : elementMappings) {
            this.getElementMappings().add(em);
        }
    }

    private boolean mapsQueryElement(QueryElement qe) {
        for (ElementMapping em : this.getElementMappings()) {
            if (em.queryElement == qe) {
                return true;
            }
        }
        return false;
    }

    private boolean mapsPatternElement(PatternElement pe) {
        for (ElementMapping em : this.getElementMappings()) {
            if (em.patternElement == pe) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object clone() {
        List<ElementMapping> clonedElementMappings = new LinkedList<ElementMapping>(this.getElementMappings());
        PatternToQueryMapping result = new PatternToQueryMapping();
        return new PatternToQueryMapping(this.getPattern(), clonedElementMappings);
    }

    @Override
    public String toString() {
        String result = "mapping - relevance mark = " + this.getRelevanceMark()
                + " ( query coverage = " + getrQueryCov()
                + " - pattern coverage = " + getrPatternCov()
                + " - element mappings = " + getrMap()
                + " - queried elements = " + getrNumQueried() + " )\n";
        result += getPattern().toStringWithMapping(this) + "\n";
        return result.substring(0, result.length() - 2);
    }

    // returns list because of arity of SubpatternsCollections
    public List<ElementMapping> getElementMappings(PatternElement pe) {
        List<ElementMapping> result = new LinkedList<ElementMapping>();
        for (ElementMapping petqem : this.getElementMappings()) {
            if (petqem.patternElement.equals(pe)) {
                result.add(petqem);
            }
        }
        return result;
    }

    private void evaluateRelevanceMark(Query userQuery) {

        // element mappings relevance mark
        // we assume here that all redundant query mappings have been removed (so we don't consider
        // element mappings that won't influence the final query)
        setrMap(0);
        for (ElementMapping em : this.getElementMappings()) {
            // penalize mapping of several pattern elements to one query element
            int numPe = 0;
            for (ElementMapping em2 : this.getElementMappings()) {
                /*if (em.patternElement instanceof PropertyPatternElement) {
                    PropertyPatternElement ppe = (PropertyPatternElement) em.patternElement;
                    if (!ppe.getReferedElements().contains(em2.patternElement.getId()) && em.queryElement == em2.queryElement) {
                        numPe++;
                    }
                } else if (em2.patternElement instanceof PropertyPatternElement) {
                    PropertyPatternElement ppe = (PropertyPatternElement) em2.patternElement;
                    if (!ppe.getReferedElements().contains(em.patternElement.getId()) && em2.queryElement == em.queryElement) {
                        numPe++;
                    }
                } else*/ if (em.queryElement == em2.queryElement && em.impliedBy != em2 && em2.impliedBy != em) {
                    numPe++;
                }
            }
            if (em.queryElement.isQueried()) {
                setrMap((float) (getrMap(userQuery) + em.trustMark / Math.pow(numPe, 3)));
            } else {
                setrMap((float) (getrMap(userQuery) + em.trustMark / Math.pow(numPe, 3)));
            }
        }
        if (!this.elementMappings.isEmpty()) {
            setrMap(getrMap(userQuery) / (float) (this.getElementMappings().size()));
        }

        // query coverage relevance mark
        int numMatchedElements = 0;
        for (QueryElement qe : userQuery.getQueryElements()) {
            if (this.mapsQueryElement(qe)) {
                numMatchedElements++;
            }
        }
        setrQueryCov((float) numMatchedElements / (float) userQuery.getQueryElements().size());

        // pattern coverage relevance mark
        numMatchedElements = 0;
        int numPeInFinalQuery = 0;
        for (PatternElement pe : Controller.getInstance().getPatternElementsForPattern(this.pattern)) {
            if (pe.isQualifying()) {
                if (this.mapsPatternElement(pe)) {
                    numMatchedElements++;
                }
                if (this.influencesFinalQuery(pe)) {
                    numPeInFinalQuery++;
                }
            }
        }
        setrPatternCov((float) numMatchedElements / (float) numPeInFinalQuery);

        // respect of the number of queried elements relevance mark
        int numQueriedInQuery = userQuery.getNumberOfQueriedElements();
        int numQueriedInMapping = 0;
        for (ElementMapping em : this.getElementMappings()) {
            if (em.queryElement.isQueried() && !(em.patternElement instanceof PropertyPatternElement)) {
                numQueriedInMapping++;
            }
        }
        if (numQueriedInQuery == 0) {
            setrNumQueried(1);
        } else {
            setrNumQueried(Math.min(numQueriedInQuery, numQueriedInMapping) / Math.max(numQueriedInQuery, numQueriedInMapping));
        }

        // TODO: structure respect relevance mark
        // sera utile par exemple pour: ?movie: director=josee dayan, adaptation of= $novel;
        //                              $novel: author= Fred Vargas

//        this.relevanceMark = (float) .3 * rMap + (float) .6 * rQueryCov + (float) .1 * rPatternCov;
        this.setRelevanceMark((float) (Math.pow(getrMap(userQuery), .25)
                * Math.pow(getrQueryCov(userQuery), .35)
                * Math.pow(getrPatternCov(userQuery), .25)
                * Math.pow(getrNumQueried(userQuery), .15)));
    }

    public boolean isRedundant() {
        for (ElementMapping em : this.getElementMappings()) {
            if (!influencesFinalQuery(em.getPatternElement())) {
//                System.out.println("redundant query mapping:\n" + this + "\n");
                return true;
            }
        }
        return false;
    }

    /**
     * checks wether an element mapping will influence the generation of the final graph query
     * (example of "useless" mapping: the mapping of a pattern element that appears only in a
     * subpattern collection that won't appear in the final query because its minOccurrences is 0
     * and its pivot element is not mapped)
     * @param em
     * @return
     */
    private boolean influencesFinalQuery(PatternElement pm) {
        for (Subpattern sp : this.getPattern().getSubpatterns()) {
            if (sp instanceof PatternTriple) {
                if (((PatternTriple) sp).contains(pm)) {
                    return true;
                }
            } else if (sp instanceof SubpatternCollection) {
                if (influencesFinalQueryInSubpatternCollection(pm, (SubpatternCollection) sp)) {
                    return true;
                }
            }
        }
//        System.out.println("pattern element does not influence final query:\n" + pm + "\n");
        return false;
    }

    private boolean influencesFinalQueryInSubpatternCollection(PatternElement pm, SubpatternCollection sc) {
        if (sc.getMinOccurrences() > 0 || this.mapsPatternElement(sc.getPivotElement())) {
            for (Subpattern sp : sc.getSubpatterns()) {
                if (sp instanceof PatternTriple) {
                    if (((PatternTriple) sp).contains(pm)) {
                        return true;
                    }
                } else if (sp instanceof SubpatternCollection) {
                    if (influencesFinalQueryInSubpatternCollection(pm, (SubpatternCollection) sp)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void generateSentence(SparqlServer sparqlServer, Query userQuery) {
        String localSentence = this.getPattern().getSentenceTemplate();

        localSentence = this.getPattern().modifySentence(localSentence, this, sparqlServer);

        Collection<PatternElement> replacedPatternElements = new LinkedList<PatternElement>();
        for (ElementMapping em : this.getElementMappings()) {
            QueryElement qe = em.queryElement;
            String queried = qe.isQueried() ? "?" : "";
            
            /*if(em instanceof KbElementMapping) {
                KbElementMapping kbem = (KbElementMapping) em;
                if(kbem.isClass()) {
                    localSentence = "<select><option>" + localSentence + "</option></select>";
                } else if(kbem.isInd()) {
                     localSentence = "<select><option>" + localSentence + "</option></select>";
                } else if(kbem.isProp()) {
                     localSentence = "<select><option>" + localSentence + "</option></select>";
                }
            }*/
            
            localSentence = localSentence.replaceAll("__" + em.patternElement.getId() + "__", queried + em.getStringForSentence(sparqlServer) + queried);
            replacedPatternElements.add(em.patternElement);
        }
        for (PatternElement pe : Controller.getInstance().getPatternElementsForPattern(this.pattern)) {
            if (!replacedPatternElements.contains(pe)) {
                localSentence = localSentence.replaceAll("__" + pe.getId() + "__", pe.getDefaultStringForSentence(sparqlServer));
            }
        }
        if (userQuery.isCount()) {
            localSentence = "NUMBER OF ( " + localSentence + " )";
        }
        if (userQuery.isAsk()) {
            localSentence = "ASK ( " + localSentence + " )";
        }
        this.sentence = localSentence;
    }

    private void generateSparqlQuery(SparqlServer sparqlServer, Query userQuery) {
        String prefixes = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>";
        Set<String> selectElements = new HashSet<String>();
        String where = "";
        Map<PatternElement, String> pivotsNames = new HashMap<PatternElement, String>();
        for (Subpattern sp : this.getPattern().getSubpatterns()) {
            where += sp.generateSparqlWhere(this, sparqlServer, pivotsNames, selectElements);
        }
        String select = "";
        for (String selectElement : selectElements) {
            select += selectElement + " ";
        }
        if (userQuery.isCount()) {
            select = "COUNT(" + select + ")";
        }

        if (userQuery.isAsk()) {
            this.setSparqlQuery(prefixes + "\nASK {\n" + where + "}\n");
        } else {
            this.setSparqlQuery(prefixes + "\nSELECT " + select + "\nWHERE {\n" + where + "}\n");
        }
    }
}
