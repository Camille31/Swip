package org.swip.pivotToMappings.model.patterns.subpattern;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.swip.pivotToMappings.controller.Controller;
import org.swip.pivotToMappings.model.KbTypeEnum;
import org.swip.pivotToMappings.model.patterns.Pattern;
import org.swip.pivotToMappings.model.patterns.mapping.ElementMapping;
import org.swip.pivotToMappings.model.patterns.mapping.InstanceAndClassElementMapping;
import org.swip.pivotToMappings.model.patterns.mapping.KbElementMapping;
import org.swip.pivotToMappings.model.patterns.mapping.PatternToQueryMapping;
import org.swip.pivotToMappings.model.patterns.patternElement.ClassPatternElement;
import org.swip.pivotToMappings.model.patterns.patternElement.KbPatternElement;
import org.swip.pivotToMappings.model.patterns.patternElement.LiteralPatternElement;
import org.swip.pivotToMappings.model.patterns.patternElement.PatternElement;
import org.swip.pivotToMappings.model.patterns.patternElement.PropertyPatternElement;
import org.swip.pivotToMappings.model.query.queryElement.Keyword;
import org.swip.pivotToMappings.model.query.queryElement.Literal;
import org.swip.utils.sparql.SparqlServer;

/**
 * class representing a subpattern (triple e1, e2, e3)
 */
public class PatternTriple extends Subpattern {

    private static Logger logger = Logger.getLogger(PatternTriple.class);
    private ClassPatternElement e1 = null;
    private PropertyPatternElement e2 = null;
    private PatternElement e3 = null;

    public PatternTriple() {
    }

    public PatternTriple(ClassPatternElement pe1, PropertyPatternElement pe2, PatternElement pe3) {
        e1 = pe1;
        e2 = pe2;
        e3 = pe3;
    }

    public ClassPatternElement getE1() {
        return e1;
    }

    public void setE1(ClassPatternElement e1) {
        this.e1 = e1;
    }

    public PropertyPatternElement getE2() {
        return e2;
    }

    public void setE2(PropertyPatternElement e2) {
        this.e2 = e2;
    }

    public PatternElement getE3() {
        return e3;
    }

    public void setE3(PatternElement e3) {
        this.e3 = e3;
    }

    @Override
    public String toString() {
        return " + - " + e1.toString() + "\n   - " + e2.toString() + "\n   - " + e3.toString();
    }

    @Override
    public String toStringWithMapping(PatternToQueryMapping ptqm) {
        return "   + " + e1.toStringWithMapping(ptqm)
                + "\n     " + e2.toStringWithMapping(ptqm)
                + "\n     " + e3.toStringWithMapping(ptqm);
    }

    @Override
    public String generateSparqlWhere(PatternToQueryMapping ptqm, SparqlServer sparqlServer, Map<PatternElement, String> elementsStrings, Set<String> selectElements, HashMap<String, String> numerciDataPropertyElements, LinkedList<String> typeStrings, LinkedList<String> labelStrings) {
//        LinkedList<String> typeStrings = new LinkedList<String>();
//        LinkedList<String> labelStrings = new LinkedList<String>();
        HashMap<PatternElement, String> matchNumerciDataProperty = new HashMap<PatternElement, String>();
        String sparqlE1 = sparqlForElement(e1, typeStrings, labelStrings, ptqm, sparqlServer, elementsStrings, selectElements, matchNumerciDataProperty);
        String sparqlE2 = sparqlForElement(e2, typeStrings, labelStrings, ptqm, sparqlServer, elementsStrings, selectElements, matchNumerciDataProperty);
        String sparqlE3 = sparqlForElement(e3, typeStrings, labelStrings, ptqm, sparqlServer, elementsStrings, selectElements, matchNumerciDataProperty);
        String result = "       "
                + sparqlE1 + " "
                + sparqlE2 + " "
                + sparqlE3 + ".\n";

        if (!matchNumerciDataProperty.isEmpty()) {
            numerciDataPropertyElements.put(matchNumerciDataProperty.get(e2), sparqlE3);
        }

//        for (String typeString : typeStrings) {
//            result = typeString + result;
//        }
//
//        for (String labelString : labelStrings) {
//            result = labelString + result;
//        }

        return result;
    }

    private String sparqlForElement(
            PatternElement e, 
            LinkedList<String> typeStrings, 
            LinkedList<String> labelStrings, 
            PatternToQueryMapping ptqm, 
            SparqlServer sparqlServer, 
            Map<PatternElement, String> elementsStrings, 
            Set<String> selectElements, 
            HashMap<PatternElement, String> numerciDataPropertyElements) {
        
        String elementString = elementsStrings.get(e);
        if (elementString == null) {
            List<ElementMapping> elementMappings = ptqm.getElementMappings(e);
            if (!elementMappings.isEmpty()) { // element mapped
                ElementMapping elementMapping = elementMappings.get(0);
                
                elementString = innerSparqlForElement( e, typeStrings,  labelStrings,  ptqm,  sparqlServer, 
                        selectElements,  numerciDataPropertyElements, elementMapping);
                
            } else { // element not mapped
                if (e instanceof ClassPatternElement) {
                    elementString = "?var" + ++(Subpattern.varCount);
                    String type = ((ClassPatternElement) e).getUri();
                    if (!type.equals("BlankNode")) {
                        typeStrings.add("       " + elementString + " rdf:type <" + type + ">.");
                    }
                } else if (e instanceof PropertyPatternElement) {
                    elementString = "<" + ((PropertyPatternElement) e).getUri() + ">";
                } else { // literal
                    elementString = "?var" + ++(Subpattern.varCount);
                    // TODO: eventuellemnt contraindre le type du literal avec FILTER (datatype...
                }
            }
            elementsStrings.put(e, elementString);
        }
        return elementString;
    }

    private String innerSparqlForElement(
            PatternElement e,
            LinkedList<String> typeStrings,
            LinkedList<String> labelStrings,
            PatternToQueryMapping ptqm,
            SparqlServer sparqlServer,
            Set<String> selectElements,
            HashMap<PatternElement, String> numerciDataPropertyElements,
            ElementMapping elementMapping) {

        String elementString = "";
        String varName = elementMapping.getQueryElement().getVarName();
        if (elementMapping instanceof KbElementMapping) {
            KbElementMapping kbElementMapping = (KbElementMapping) elementMapping;
            String firstlyMatchedOntResource = kbElementMapping.getFirstlyMatchedOntResourceUri();

            String toInsert = "";
            if (kbElementMapping.isGeneralized() && !kbElementMapping.isNumericDataProperty()) {
                toInsert = "_gen" + kbElementMapping.getPatternElement().getId() + "_";
            } else {
                toInsert = "<" + kbElementMapping.getFirstlyMatchedOntResourceUri() + ">";
            }

            if (sparqlServer.isClass(firstlyMatchedOntResource)) { // class
                //elementString = "?var" + ++(Subpattern.varCount);
                elementString = varName;
                String typeDeclaration = "       " + elementString + " rdf:type " + toInsert + ".\n";
                if (!typeStrings.contains(typeDeclaration)) {
                    typeStrings.add(typeDeclaration);
                }
                if (kbElementMapping.getQueryElement().isQueried()) {
                    selectElements.add(elementString);
                }
            } else if (Keyword.pseudoClasses.containsKey(firstlyMatchedOntResource)) { // pseudoclass
                elementString = varName;
                String typeDeclaration = "       " + elementString + " " + Keyword.jokerTypePropertiesString + " " + toInsert + ".\n";
                if (!typeStrings.contains(typeDeclaration)) {
                    typeStrings.add(typeDeclaration);
                }
                if (kbElementMapping.getQueryElement().isQueried()) {
                    selectElements.add(elementString);
                }
            } else if (kbElementMapping.getKbType() == KbTypeEnum.NUMDATAPROP) {
                numerciDataPropertyElements.put(e, kbElementMapping.getQueryElement().getStringValue());
                elementString = toInsert;
            } else if (sparqlServer.isProperty(firstlyMatchedOntResource)) { // property
                elementString = toInsert;
            } else { // instance
                //elementString = "?var" + ++(Subpattern.varCount);
                elementString = varName;
                List<String> types = sparqlServer.listTypes(firstlyMatchedOntResource);
                for (String type : types) {
                    String typeDeclaration = "       " + elementString + " rdf:type " + toInsert + ".\n";
                    if (!typeStrings.contains(typeDeclaration)) {
                        typeStrings.add(typeDeclaration);
                    }
                }
                String matchedLabel = ((KbElementMapping) ptqm.getElementMappings(e).get(0)).getBestLabel();
                // FIXME: solve language tag problem
                labelStrings.add("       " + elementString + " (rdfs:label|dc:title|foaf:name) \"" + matchedLabel + "\".\n");
            }
        } else if (elementMapping instanceof InstanceAndClassElementMapping) {
            elementString = varName;
            typeStrings.add("       " + elementString + " " + Keyword.jokerTypePropertiesStringWithType + " <" + ((InstanceAndClassElementMapping) elementMapping).getFirstlyMatchedClass() + ">.\n");
            labelStrings.add("       " + elementString + " (rdfs:label|dc:title|foaf:name) \"" + ((InstanceAndClassElementMapping) elementMapping).getBestLabelInstance() + "\".\n");
        } else { // literal
            if (elementMapping.getQueryElement().isQueried()) {
//                        elementString = "?literal" + ++(Subpattern.varCount);
                elementString = varName;
                // TODO: eventuellemnt contraindre le type du literal avec FILTER (datatype...
                selectElements.add(elementString);
            } else {
                elementString = ((Literal) elementMapping.getQueryElement()).getStringForSparql(labelStrings);
            }
        }
        return elementString;

    }

    public boolean contains(PatternElement pe) {
        return (this.e1 == pe || this.e2 == pe || this.e3 == pe);
    }

    @Override
    public void finalizeMapping(SparqlServer serv, Pattern p) {
        // handle pattern elements refered by property pattern elements
        List<PatternElement> elementsToMap = new LinkedList<PatternElement>();
        for (int id : this.e2.getReferedElements()) {
            elementsToMap.add(p.getPatternElementById(id));
        }
        for (ElementMapping em2 : Controller.getInstance().getElementMappingsForPatternElement(this.e2)) {
            if (!em2.getQueryElement().getRoles().usedAsProperty()) {
                for (PatternElement elementToMap : elementsToMap) {
                    if (elementToMap instanceof KbPatternElement) {
                        KbPatternElement kbe = (KbPatternElement) elementToMap;
                        kbe.addKbMapping(em2.getQueryElement(), em2.getTrustMark(), kbe.getUri(), serv.getALabel(kbe.getUri()), em2, KbTypeEnum.CLASS);
                    } else if (elementToMap instanceof LiteralPatternElement) {
                        LiteralPatternElement le = (LiteralPatternElement) elementToMap;
                        le.addLiteralMapping(em2.getQueryElement(), em2.getTrustMark(), em2);
                    }
                }
            }
        }
    }
}