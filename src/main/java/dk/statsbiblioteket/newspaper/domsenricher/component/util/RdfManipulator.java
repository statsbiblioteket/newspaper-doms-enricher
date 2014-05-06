package dk.statsbiblioteket.newspaper.domsenricher.component.util;

import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import javax.xml.transform.TransformerException;

/**
 * Simple class for manipulating xml representation of rdf datastream.
 */
public class RdfManipulator {
    Document document;
    Node rdfDescriptionNode;

    public static final String MODEL_TEMPLATE =  "<hasModel xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
            + "xmlns=\"info:fedora/fedora-system:def/model#\" "
            + "rdf:resource=\"info:fedora/PID\"/>";

    public static final String EXTERNAL_RELATION_TEMPLATE = "<NAME xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
            + "xmlns=\"http://doms.statsbiblioteket.dk/relations/default/0/1/#\" "
            + "rdf:resource=\"info:fedora/PID\"/>";

    public static final XPathSelector X_PATH_SELECTOR = DOM.createXPathSelector(
            "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "doms", "http://doms.statsbiblioteket.dk/relations/default/0/1/#",
            "relsExt", "info:fedora/fedora-system:def/relations-external#"
    );

    /**
     * CTOR for the class
     * @param xml String representation of the rdf datastream,
     */
    public RdfManipulator(String xml) {
        document = DOM.stringToDOM(xml, true);
        rdfDescriptionNode = X_PATH_SELECTOR.selectNode(document, "//rdf:Description");
    }

    /**
     * Returns the rdf datastream xml as a String.
     * @return the manipulated datastream.
     */
    @Override
    public String toString() {
        try {
            return DOM.domToString(document);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Incorporates the given fragment of rdf/xml as a rdf:description node of the given document
     * @param fragment The rdf/xml fragment to be added
     */
    private void addFragmentToDescription(Fragment fragment) {
        if(!containsFragment(fragment)) {
            Document fragmentNode = DOM.stringToDOM(fragment.toString(), true);
            Node importedNode = document.importNode(fragmentNode.getDocumentElement(), true);
            rdfDescriptionNode.appendChild(importedNode);
        }
    }
    
    private boolean containsFragment(Fragment fragment) {
        XPathSelector selector = DOM.createXPathSelector("our", fragment.getPredicate(), 
                "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        String xpath = "//our:" + fragment.getPredicateName() 
                + "[@rdf:resource='info:fedora/" + fragment.getObject() + "']";
        Node node = selector.selectNode(rdfDescriptionNode, xpath);
        if(node == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Adds a content model.
     * @param modelPid the full pid of the model, including prefix e.g. "doms:ContentModel_DOMS".
     */
    public RdfManipulator addContentModel(String modelPid) {
        return addRelation("info:fedora/fedora-system:def/model#", "hasModel", modelPid);
    }

    /**
     * Adds a new named relation from this object to another object, e.g.
     * <hasPart xmlns="info:fedora/fedora-system:def/relations-external#" rdf:resource="info:fedora/uuid:05d840bf-8bb6-48e5-b214-2ab39f6259f8"/>
     * @param predicateName the short name of the relation, e.g. "hasPart"
     * @param objectPid the full doms pid of the object of the relation, e.g. "uuid:05d840bf-8bb6-48e5-b214-2ab39f6259f8"
     */
    public RdfManipulator addExternalRelation(String predicateName, String objectPid) {
        return addRelation("info:fedora/fedora-system:def/relations-external#", predicateName, objectPid);
    }
    
    /**
     * Adds a new named relation from this object to another object, e.g.
     * <hasPart xmlns="http://doms.statsbiblioteket.dk/relations/default/0/1/#" rdf:resource="info:fedora/uuid:05d840bf-8bb6-48e5-b214-2ab39f6259f8"/>
     * @param predicateName the short name of the relation, e.g. "hasPart"
     * @param objectPid the full doms pid of the object of the relation, e.g. "uuid:05d840bf-8bb6-48e5-b214-2ab39f6259f8"
     */
    public RdfManipulator addDomsRelation(String predicateName, String objectPid) {
        return addRelation("http://doms.statsbiblioteket.dk/relations/default/0/1/#", predicateName, objectPid);
        
    }
    
    private RdfManipulator addRelation(String predicateNS, String predicateName, String objectPid) {
        Fragment frag = new Fragment(predicateNS, predicateName, objectPid);
        addFragmentToDescription(frag);
        return this;
    }
    
    private class Fragment {
        private String predicate;
        private String predicateName;
        private String object;
        
        Fragment(String predicate, String predicateName, String object) {
            this.predicate = predicate;
            this.predicateName = predicateName;
            this.object = object;
        }
        
        public String getPredicate() {
            return predicate;
        }

        public String getPredicateName() {
            return predicateName;
        }

        public String getObject() {
            return object;
        }

        public String toString() {
            return "<" + predicateName + " xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
                    + "xmlns=\"" + predicate + "\" rdf:resource=\"info:fedora/" + object + "\"/>"; 
        }
    }
}
