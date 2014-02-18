package dk.statsbiblioteket.newspaper.domsenricher.component;

import dk.statsbibliokeket.newspaper.treenode.TreeNodeStateWithChildren;
import dk.statsbibliokeket.newspaper.treenode.TreeNodeWithChildren;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.medieplatform.autonomous.ResultCollector;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.common.AttributeParsingEvent;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.common.NodeBeginsParsingEvent;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.common.NodeEndParsingEvent;
import dk.statsbiblioteket.medieplatform.autonomous.iterator.eventhandlers.TreeEventHandler;
import dk.statsbiblioteket.newspaper.domsenricher.component.enrichers.AbstractNodeEnricher;
import dk.statsbiblioteket.newspaper.domsenricher.component.enrichers.NodeEnricherFactory;
import dk.statsbiblioteket.newspaper.treenode.TreeNodeState;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.List;

/**
 * A tree handler which enriches each node with the relevant content models.
 */
public class DomsEnricherTreeEventHandler extends TreeNodeStateWithChildren {

    private static Logger logger = LoggerFactory.getLogger(DomsEnricherTreeEventHandler.class);

    private ResultCollector resultCollector;
    private NodeEnricherFactory nodeEnricherFactory;
    private EnhancedFedora fedora;

    public DomsEnricherTreeEventHandler(EnhancedFedora fedora, ResultCollector resultCollector) {
        nodeEnricherFactory = new NodeEnricherFactory(fedora);
        this.resultCollector = resultCollector;
        this.fedora = fedora;
    }


    /**
     * Does the actual enrichment by adding all necessary relationships to child-nodes, content-models,
     * and licenses (?).
     * @param event the event identifying this node.
     */
    @Override
    public void processNodeEnd(NodeEndParsingEvent event) {

        final AbstractNodeEnricher nodeEnricher = nodeEnricherFactory.getNodeEnricher(getCurrentNode().getType());
        try {
            nodeEnricher.enrich(event);
        } catch (Exception e) {
            resultCollector.addFailure(event.getName(), "exception", DomsEnricherComponent.class.getSimpleName(), e.getMessage());
        }
        String contentModelXml = nodeEnricher.getRelsExtFragment();
        StringBuilder childRelationships = new StringBuilder();
        childRelationships.append(contentModelXml);
        List<TreeNodeWithChildren> children = ((TreeNodeWithChildren) getCurrentNode()).getChildren();
        for (TreeNodeWithChildren childNode: children) {
            String elementName = null;
            switch (childNode.getType()) {
                case BATCH:
                    throw new IllegalStateException("Unexpectedly found a batch node" + childNode.getName() + "as the child of " +  event.getName());
                case WORKSHIFT_ISO_TARGET:
                    elementName = "hasWorkshift";
                    break;
                case WORKSHIFT_TARGET:
                    elementName = "hasPage";
                    break;
                case TARGET_IMAGE:
                    elementName = "hasPage";
                    break;
                case FILM:
                    elementName = "hasFilm";
                    break;
                case FILM_ISO_TARGET:
                    elementName = "hasIsoTarget";
                    break;
                case FILM_TARGET:
                    elementName = "hasPage";
                    break;
                case ISO_TARGET_IMAGE:
                    elementName = "hasFile";
                    break;
                case UNMATCHED:
                    elementName = "hasPage";
                    break;
                case EDITION:
                    elementName = "hasEdition";
                    break;
                case PAGE:
                    elementName = "hasPage";
                    break;
                case BRIK:
                    elementName = "hasBrik";
                    break;
                case BRIK_IMAGE:
                    elementName = "hasFile";
                    break;
                case PAGE_IMAGE:
                    elementName = "hasFile";
                    break;
            }
            if (elementName != null) {
                childRelationships.append("<");
                childRelationships.append(elementName);
                childRelationships.append(" xmlns=\"info:fedora/fedora-system:def/relations-external#\" rdf:resource=\"info:fedora/");
                childRelationships.append(childNode.getLocation());
                childRelationships.append("\"/>\n");
            }
        }
        logger.debug("Should add rdf\n" + childRelationships.toString());
        try {
            //Replace with DOM manipulation that adds one node at a time.
            /*String relsExtXmlString = fedora.getXMLDatastreamContents(event.getLocation(), "RELS-EXT");
            Document relsExtDocument = DOM.stringToDOM(relsExtXmlString);
            Node rdfFragment = DOM.stringToDOM(childRelationships.toString());
            relsExtDocument.importNode(rdfFragment, true);
            String xpath = "RDF/Description";
            XPathSelector xPathSelector = DOM.createXPathSelector();
            Node descriptionNode = xPathSelector.selectNode(relsExtDocument, xpath);
            descriptionNode.appendChild(rdfFragment);
            logger.debug("new rdf:\n" + DOM.domToString(relsExtDocument));*/
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
