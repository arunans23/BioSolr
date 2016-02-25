/**
 * Copyright (c) 2016 Lemur Consulting Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.flax.biosolr.ontology.core;

import java.util.List;

/**
 * Base configuration details for the OntologyHelper
 * implementations.
 *
 * Created by mlp on 23/02/16.
 * @author mlp
 */
public class OntologyHelperConfiguration {

	public static final String NODE_PATH_SEPARATOR = ",";
	public static final String NODE_LABEL_SEPARATOR = " => ";

	private String nodePathSeparator = NODE_PATH_SEPARATOR;
	private String nodeLabelSeparator = NODE_LABEL_SEPARATOR;

	public String getNodePathSeparator() {
		return nodePathSeparator;
	}

	public void setNodePathSeparator(String nodePathSeparator) {
		this.nodePathSeparator = nodePathSeparator;
	}

	public String getNodeLabelSeparator() {
		return nodeLabelSeparator;
	}

	public void setNodeLabelSeparator(String nodeLabelSeparator) {
		this.nodeLabelSeparator = nodeLabelSeparator;
	}

}
