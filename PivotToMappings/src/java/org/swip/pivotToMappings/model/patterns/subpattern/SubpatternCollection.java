package org.swip.pivotToMappings.model.patterns.subpattern;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.swip.pivotToMappings.model.KbTypeEnum;
import org.swip.pivotToMappings.model.patterns.Pattern;
import org.swip.pivotToMappings.model.patterns.mapping.ElementMapping;
import org.swip.pivotToMappings.model.patterns.mapping.KbElementMapping;
import org.swip.pivotToMappings.model.patterns.mapping.LiteralElementMapping;
import org.swip.pivotToMappings.model.patterns.mapping.PatternToQueryMapping;
import org.swip.pivotToMappings.model.patterns.patternElement.PatternElement;
import org.swip.pivotToMappings.model.query.queryElement.Keyword;
import org.swip.pivotToMappings.model.query.queryElement.Literal;
import org.swip.pivotToMappings.model.query.queryElement.QueryElement;
import org.swip.utils.sparql.SparqlServer;

public class SubpatternCollection extends Subpattern {

    private static Logger logger = Logger.getLogger(SubpatternCollection.class);
    Collection<Subpattern> subpatterns = new LinkedList<Subpattern>();
    String id = null;
    private int minOccurrences = 1;
    private int maxOccurrences = 1;
    private PatternElement pivotElement = null;

    public SubpatternCollection() {
    }

    public SubpatternCollection(Collection<Subpattern> subpatterns, String id, int minOccurrences, int maxOccurrences, PatternElement pivotElement) {
        this.subpatterns = subpatterns;
        this.id = id;
        this.minOccurrences = minOccurrences;
        this.maxOccurrences = maxOccurrences;
        this.pivotElement = pivotElement;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMaxOccurrences() {
        return maxOccurrences;
    }

    public void setMaxOccurrences(int maxOccurrences) {
        this.maxOccurrences = maxOccurrences;
    }

    public int getMinOccurrences() {
        return minOccurrences;
    }

    public void setMinOccurrences(int minOccurrences) {
        this.minOccurrences = minOccurrences;
    }

    public PatternElement getPivotElement() {
        return pivotElement;
    }

    public void setPivotElement(PatternElement pivotElement) {
        this.pivotElement = pivotElement;
    }

    public Collection<Subpattern> getSubpatterns() {
        return subpatterns;
    }

    public void setSubpatterns(Collection<Subpattern> subpatterns) {
        this.subpatterns = subpatterns;
    }

    @Override
    public String toString() {
        String result = "[\n";
        for (Subpattern sp : this.subpatterns) {
            result += sp.toString() + "\n";
        }
        result += "] " + this.id + ": ";
        if (minOccurrences == maxOccurrences) {
            result += "exactly " + minOccurrences;
        } else {
            if (maxOccurrences == -1) {
                result += " " + minOccurrences + " or more";
            } else {
                result += "between " + minOccurrences + " and " + maxOccurrences;
            }
        }
        if (maxOccurrences > 1 || maxOccurrences == -1) {
            result += " through " + pivotElement.toString();
        }
        return result;
    }

    public String toStringWithMapping(PatternToQueryMapping ptqm) {
        String result = "";
        for (Subpattern sp : this.subpatterns) {
            result += sp.toStringWithMapping(ptqm) + "\n";
        }
        return result.substring(0, result.length() - 1);
    }

//    public boolean containsAsNonPivot(PatternElement pe) {
//        if (this.pivotElement == pe) {
//            return false;
//        }
//        for (Subpattern sp : this.subpatterns) {
//            if (sp.containsAsNonPivot(pe)) {
//                return true;
//            }
//        }
//        return false;
//    }
    public String modifySentence(String sentence, PatternToQueryMapping ptqm, SparqlServer sparqlServer) {
        while (true) {
            String labelBegin = "_if_" + this.id + "_[";
            String labelEnd = "]_end_if_" + this.id + "_";
            int beginIndex = sentence.indexOf(labelBegin);
            int endIndex = sentence.indexOf(labelEnd);
            if (beginIndex == -1 || endIndex == -1) {
                break;
            } else {
                String replacementString = sentence.substring(beginIndex + labelBegin.length(), endIndex);
                if (this.pivotElement != null) {
                    replacementString = generateStringReplacementLoop(replacementString, ptqm, sparqlServer);
                }
                sentence = "" + sentence.substring(0, beginIndex) + replacementString + sentence.substring(endIndex + labelEnd.length());
            }
        }
        for (Subpattern sp : this.subpatterns) {
            if (sp instanceof SubpatternCollection) {
                sentence = ((SubpatternCollection) sp).modifySentence(sentence, ptqm, sparqlServer);
            }
        }
        return sentence;
    }

    private String generateStringReplacementLoop(String s, PatternToQueryMapping ptqm, SparqlServer sparqlServer) {
        Collection<ElementMapping> ems = ptqm.getElementMappings(this.pivotElement);
        if (ems.isEmpty()) {
            // pivot element is not mapped: we don't display the phrase linked to this subpattern collection
            return "";
        } else {
            String result = "";
            String labelBegin = "_loop_" + this.id + "_(";
            String labelEnd = ")_end_loop_" + this.id + "_";
            int beginIndex = s.indexOf(labelBegin);
            int endIndex = s.indexOf(labelEnd);
            if (beginIndex == -1 || endIndex == -1) {
                return s;
            } else {
                result += s.substring(0, beginIndex);
                for (ElementMapping em : ems) {
                    QueryElement qe = em.getQueryElement();
                    String queried = "";
                    if (qe.isQueried()) {
                        queried = "?";
                    }
                    result += s.substring(beginIndex + labelBegin.length(), endIndex).replaceAll("__" + em.getPatternElement().getId() + "__", queried + em.getStringForSentence(sparqlServer, "en") + queried);
                }
            }
            result += s.substring(endIndex + labelEnd.length());
            return result;
        }
    }

    @Override
    public String generateSparqlWhere(PatternToQueryMapping ptqm, SparqlServer sparqlServer, Map<PatternElement, String> elementsStrings, Set<String> selectElements, HashMap<String, String> numerciDataPropertyElements, LinkedList<String> typeStrings, LinkedList<String> labelStrings) {
        
        
        // if subpattern collection is optional and pivot element is not mapped
        Collection<ElementMapping> pivotElementMappings = ptqm.getElementMappings(this.pivotElement);
        if (this.minOccurrences == 0 && pivotElementMappings.isEmpty()) {
            return "";
        }
        String result = "";
        // TODO: factoriser cette partie avec celle de PatternTriple
        for (ElementMapping pivotElementMapping : pivotElementMappings) {
            String pivotVarName = pivotElementMapping.getQueryElement().getVarName();
            if (pivotElementMapping instanceof KbElementMapping) {
                KbElementMapping pivotKbElementMapping = (KbElementMapping) pivotElementMapping;
                String firstlyMatchedOntResource = pivotKbElementMapping.getFirstlyMatchedOntResourceUri();

                String toInsert = "";
                if (pivotKbElementMapping.isGeneralized()) {
                    toInsert = "_gen" + pivotKbElementMapping.getPatternElement().getId() + "_";
                } else {
                    toInsert = "<" + pivotKbElementMapping.getFirstlyMatchedOntResourceUri() + ">";
                }

                if (sparqlServer.isClass(firstlyMatchedOntResource)) { // class
                    //pivotVarName = "?var" + ++(Subpattern.varCount);
                    result += "       " + pivotVarName + " rdf:type " + toInsert + ".\n";
                    if (pivotKbElementMapping.getQueryElement().isQueried()) {
                        selectElements.add(pivotVarName);
                    }
                } else if (Keyword.pseudoClasses.containsKey(firstlyMatchedOntResource)) { // pseudoclass
                    // FIXME: ce cas n'arrive peut-être jamais, à vérifier
                    result += "SI T'AS ÇA DANS LA REQUÊTE, ALORS Y'A UN PROBLÈME!!\n";
                } else if (pivotKbElementMapping.getKbType() == KbTypeEnum.NUMDATAPROP) {
                    numerciDataPropertyElements.put(pivotKbElementMapping.getBestLabel(), "loutre");
                    pivotVarName = toInsert;
                } else if (sparqlServer.isProperty(firstlyMatchedOntResource)) { // property
                    pivotVarName = toInsert;
                } else { // instance
                    //pivotVarName = "?var" + ++(Subpattern.varCount);
                    List<String> types = sparqlServer.listTypes(firstlyMatchedOntResource);
//                    for (String type : types) {
                    result += "       " + pivotVarName + " rdf:type " + toInsert + ".\n";
//                    }
                    String matchedLabel = ((KbElementMapping) pivotElementMapping).getBestLabel();
                    /*result += "       { " + pivotVarName + " <http://purl.org/dc/elements/1.1/title> \"" + matchedLabel + "\". } "
                    + "UNION "
                    + "{ " + pivotVarName + " rdfs:label \"" + matchedLabel + "\". }\n";*/
                    result += "       " + pivotVarName + " (rdfs:label|dc:title|foaf:name) \"" + matchedLabel + "\".\n";
                }
            } else if (pivotElementMapping instanceof LiteralElementMapping) { // literal
                if (pivotElementMapping.getQueryElement().isQueried()) {
                    //pivotVarName = "?literal" + ++(Subpattern.varCount);
                    // TODO: eventuellemnt contraindre le type du literal avec FILTER (datatype...
                    selectElements.add(pivotVarName);
                } else {
//                    List<String> typeStrings = new LinkedList<String>();
                    pivotVarName = pivotElementMapping.getQueryElement().getVarName();
                    Literal l = (Literal) (pivotElementMapping.getQueryElement());
                    String filterString = "FILTER ( " + pivotVarName + " = \"" + l.getStringValue() + "\"^^FIXME )";
                    labelStrings.add(filterString);
//                    for (String typeString : typeStrings) {
//                        result += typeString;
//                    }
                }
            }
            elementsStrings.put(this.pivotElement, pivotVarName);
            for (Subpattern sp : this.subpatterns) {
                result += sp.generateSparqlWhere(ptqm, sparqlServer, elementsStrings, selectElements, numerciDataPropertyElements, typeStrings, labelStrings);
            }
        }
        // if pivot element was not mapped but minOccurrences>0
        if (result.equals("")) {
            for (Subpattern sp : this.subpatterns) {
                result += sp.generateSparqlWhere(ptqm, sparqlServer, elementsStrings, selectElements, numerciDataPropertyElements, typeStrings, labelStrings);
            }
        }

        elementsStrings.remove(this.pivotElement);

        return result;
    }

    @Override
    public void finalizeMapping(SparqlServer serv, Pattern p) {
        for (Subpattern sp : this.subpatterns) {
            sp.finalizeMapping(serv, p);
        }
    }
}
