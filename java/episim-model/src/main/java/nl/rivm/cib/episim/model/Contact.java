/* $Id: cb08b056da27db5a480015849bd8dff2d15e7051 $
 * 
 * Part of ZonMW project no. 50-53000-98-156
 * 
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Copyright (c) 2016 RIVM National Institute for Health and Environment 
 */
package nl.rivm.cib.episim.model;

import io.coala.name.AbstractIdentifiable;
import io.coala.name.AbstractIdentifier;

/**
 * {@link Contact} represents a link between two {@link Subject}s in the physical contact network
 * 
 * @version $Id: cb08b056da27db5a480015849bd8dff2d15e7051 $
 * @author <a href="mailto:rick.van.krevelen@rivm.nl">Rick van Krevelen</a>
 *
 */
@SuppressWarnings("serial")
public class Contact extends AbstractIdentifiable<Contact.ID>
{
	static class ID extends AbstractIdentifier<String>
	{
		
	}
	
	public ID id;

	public Subject person;
}