/**
 * Copyright (c) 2015 Lemur Consulting Ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.flax.biosolr.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;

import uk.co.flax.biosolr.FacetTreeBuilder;
import uk.co.flax.biosolr.HierarchicalFacets;

/**
 * JavaDoc for AbstractNodeFacetTreeBuilder.
 *
 * @author mlp
 */
public abstract class AbstractFacetTreeBuilder implements FacetTreeBuilder {
	
	private String nodeField;
	private String labelField;

	@Override
	public void initialiseParameters(SolrParams localParams) throws SyntaxError {
		getLogger().trace("Initialising parameters...");
		if (localParams == null) {
			throw new SyntaxError("Missing facet tree parameters");
		}
		
		// Initialise the node field - REQUIRED
		nodeField = localParams.get(HierarchicalFacets.NODE_FIELD_PARAM);
		if (StringUtils.isBlank(nodeField)) {
			// Not specified in localParams - use the key value instead
			nodeField = localParams.get(QueryParsing.V);
			
			// If still blank, we have a problem
			if (StringUtils.isBlank(nodeField)) {
				throw new SyntaxError("No node field defined in " + localParams);
			}
		}

		//  Initialise the optional fields
		labelField = localParams.get(HierarchicalFacets.LABEL_FIELD_PARAM, null);
	}
	
	protected void checkFieldsInSchema(SolrIndexSearcher searcher, String... fields) throws SolrException {
		IndexSchema schema = searcher.getSchema();
		for (String field : fields) {
			SchemaField sField = schema.getField(field);
			if (sField == null) {
				throw new SolrException(ErrorCode.BAD_REQUEST, "\"" + field
						+ "\" is not in schema " + schema.getSchemaName());
			}
		}
	}
	
	protected String getNodeField() {
		return nodeField;
	}
	
	protected String getLabelField() {
		return labelField;
	}
	
	protected abstract Logger getLogger();
	
}
