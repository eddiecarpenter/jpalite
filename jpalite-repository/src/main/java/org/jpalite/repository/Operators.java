/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jpalite.repository;

public enum Operators
{
	EQUALS("Equal to", 0, "=", "", "", 1),
	NOTEQUALS("Not Equal to", 0, "!=", "", "", 1),
	BEGINS_WITH("Begins with", 1, "ilike", "", "%", 1),
	ENDS_WITH("Ends with", 1, "ilike", "%", "", 1),
	CONTAINS("Contains", 1, "ilike", "%", "%", 1),
	CONTAINS_NOT("Does Not Contain", 1, "not ilike", "%", "%", 1),
	BIGGER_THAN("Bigger than", 2, ">", "", "", 1),
	BIGGER_OR_EQUAL("Bigger than or equal to", 2, ">=", "", "", 1),
	SMALLER_THAN("Smaller than", 2, "<", "", "", 1),
	SMALLER_OR_EQUAL("Smaller than or equal to", 2, "<=", "", "", 1),
	ISNULL("Is Not Set", 0, "is null", "", "", 0),
	ISNOTNULL("Is Set", 0, "is not null", "", "", 0),
	BETWEEN("Between", 1, "between", "", "", 2),
	PLUS_INTERVAL("+ Interval", 3, "interval", "", "", 1),
	MINUS_INTERVAL("- Interval", 3, "interval", "", "", 1),
	IN("In", 3, "in", "(", ")", -1),
	NOTIN("Not In", 3, "not in", "(", ")", -1);

	private final String comparator;
	private final int category;
	private final String label;
	private final String prefix;
	private final String postfix;
	private final int nrvalues;

	private Operators(String label, int category, String comparator, String prefix, String postfix, int nrValues)
	{
		this.label = label;
		this.category = category;
		this.comparator = comparator;
		this.prefix = prefix;
		this.postfix = postfix;
		nrvalues = nrValues;
	}//Operators

	public String getLabel()
	{
		return label;
	}

	public int getNrValues()
	{
		return nrvalues;
	}

	public int getCategory()
	{
		return category;
	}

	public String getOperator()
	{
		return comparator;
	}

	public String getPrefix()
	{
		return prefix;
	}

	public String getPostfix()
	{
		return postfix;
	}
}//Operators
