package uk.co.flax.biosolr.ontology.search.solr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.api.AccumulatedFacetEntry;
import uk.co.flax.biosolr.ontology.api.FacetEntry;
import uk.co.flax.biosolr.ontology.api.OntologyEntryBean;
import uk.co.flax.biosolr.ontology.search.OntologySearch;
import uk.co.flax.biosolr.ontology.search.ResultsList;
import uk.co.flax.biosolr.ontology.search.SearchEngineException;

public class ChildNodeFacetTreeBuilder implements FacetTreeBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChildNodeFacetTreeBuilder.class);
	
	private final OntologySearch ontologySearch;

	public ChildNodeFacetTreeBuilder(OntologySearch ontologySearch) {
		this.ontologySearch = ontologySearch;
	}
	
	@Override
	public List<FacetEntry> buildFacetTree(List<FacetEntry> entries) {
		// Extract the URIs from the facet entries
		Set<String> uriSet = extractIdsFromFacets(entries);
		
		// Look up all nodes with those URIs in their child list
		Map<String, OntologyEntryBean> annotationMap = lookupOntologyEntriesByUri(uriSet, true);
		
		// Look up nodes all the way up the tree
		Set<String> lookupSet = extractIdsFromAnnotations(annotationMap.values());
		while (lookupSet.size() > 0) {
			Map<String, OntologyEntryBean> lookupMap = lookupOntologyEntriesByUri(lookupSet, true);
			annotationMap.putAll(lookupMap);
			
			lookupSet = extractIdsFromAnnotations(lookupMap.values());
			lookupSet.removeAll(annotationMap.keySet());
		}
		
		// Look up the very bottom-level entries
		uriSet.removeAll(annotationMap.keySet());
		// Look up by URI, rather than child URI
		annotationMap.putAll(lookupOntologyEntriesByUri(uriSet, false));
		
		// Find the top node(s)
		Set<String> topUris = findTopLevelNodes(annotationMap);
		
		// Convert the original facets to a map
		Map<String, FacetEntry> entryMap = 
				entries.stream().collect(Collectors.toMap(FacetEntry::getLabel, Function.identity()));

		// Now collate the nodes into level-based tree(s)
		List<FacetEntry> facetTrees = new ArrayList<>(topUris.size());
		for (String uri : topUris) {
			FacetEntry fe = buildAccumulatedEntryTree(0, annotationMap.get(uri), entryMap, annotationMap);
			facetTrees.add(fe);
		}
		
		return facetTrees;
	}
	
	private Set<String> extractIdsFromFacets(List<FacetEntry> entries) {
		return entries.stream().map(FacetEntry::getLabel).collect(Collectors.toSet());
	}
	
	private Set<String> extractIdsFromAnnotations(Collection<OntologyEntryBean> annotations) {
		return annotations.stream().map(OntologyEntryBean::getUri).collect(Collectors.toSet());
	}
	
	/**
	 * Fetch the EFO annotations for a collection of URIs.
	 * @param uris
	 * @return a map of URI -> ontology entry for the incoming URIs.
	 */
	private Map<String, OntologyEntryBean> lookupOntologyEntriesByUri(Collection<String> uris, boolean children) {
		Map<String, OntologyEntryBean> annotationMap = new HashMap<>();
		if (uris.size() > 0) {
			LOGGER.debug("Looking up {} entries by URI", uris.size());

			String query = "*:*";
			String filters = buildFilterString(children ? SolrOntologySearch.CHILD_URI_FIELD : SolrOntologySearch.URI_FIELD, uris);

			try {
				ResultsList<OntologyEntryBean> results = ontologySearch.searchOntology(query, Arrays.asList(filters), 0, uris.size());
				annotationMap = results.getResults().stream().collect(Collectors.toMap(OntologyEntryBean::getUri, Function.identity()));
			} catch (SearchEngineException e) {
				LOGGER.error("Problem getting ontology entries for filter {}: {}", filters, e.getMessage());
			}
		}
		
		return annotationMap;
	}
	
	/**
	 * Build a filter string for a set of URIs.
	 * @param uris
	 * @return a filter string.
	 */
	private String buildFilterString(String field, Collection<String> uris) {
		StringBuilder sb = new StringBuilder(field).append(":(");
		
		int idx = 0;
		for (String uri : uris) {
			if (idx > 0) {
				sb.append(" OR ");
			}
			
			sb.append("\"").append(uri).append("\"");
			idx ++;
		}
		sb.append(")");
		
		return sb.toString();
	}
	
	private Set<String> findTopLevelNodes(Map<String, OntologyEntryBean> annotations) {
		Set<String> topLevel = new HashSet<>();

		for (String uri : annotations.keySet()) {
			boolean found = false;
			
			// Check each annotation in the set to see if this
			// URI is in their child list
			for (OntologyEntryBean anno : annotations.values()) {
				if (anno.getChildUris() != null && anno.getChildUris().contains(uri)) {
					// URI is in the child list - not top-level
					found = true;
					break;
				}
			}
			
			if (!found) {
				// URI Is not in any child lists - must be top-level
				topLevel.add(uri);
			}
		}
		
		return topLevel;
	}
	
	/**
	 * Recursively build an accumulated facet entry tree.
	 * @param level current level in the tree (used for debugging/logging).
	 * @param node the current node.
	 * @param entryMap the facet entry map.
	 * @param annotationMap the map of valid annotations (either in the facet map, or parents of
	 * entries in the facet map).
	 * @return an {@link AccumulatedFacetEntry} containing details for the current node and all
	 * sub-nodes down to the lowest leaf which has a facet count.
	 */
	private AccumulatedFacetEntry buildAccumulatedEntryTree(int level, OntologyEntryBean node,
			Map<String, FacetEntry> entryMap, Map<String, OntologyEntryBean> annotationMap) {
		SortedSet<AccumulatedFacetEntry> childHierarchy = new TreeSet<>(Collections.reverseOrder());
		long childTotal = 0;
		if (node.getChildUris() != null) {
			for (String childUri : node.getChildUris()) {
				if (annotationMap.containsKey(childUri)) {
					LOGGER.trace("[{}] Building subAfe for {}", level, childUri);
					AccumulatedFacetEntry subAfe = buildAccumulatedEntryTree(level + 1, annotationMap.get(childUri),
							entryMap, annotationMap);
					childTotal += subAfe.getTotalCount();
					childHierarchy.add(subAfe);
					LOGGER.trace("[{}] subAfe total: {} - child Total {}, child count {}", level, subAfe.getTotalCount(), childTotal, childHierarchy.size());
				}
			}
		}

		long count = 0;
		if (entryMap.containsKey(node.getUri())) {
			count = entryMap.get(node.getUri()).getCount();
		}
		
		String label;
		if (node.getLabel() == null) {
			label = node.getShortForm().get(0);
		} else {
			label = node.getLabel().get(0);
		}
		
		LOGGER.trace("[{}] Building AFE for {}", level, node.getUri());
		return new AccumulatedFacetEntry(node.getUri(), label, count, childTotal, childHierarchy);
	}
	
}
