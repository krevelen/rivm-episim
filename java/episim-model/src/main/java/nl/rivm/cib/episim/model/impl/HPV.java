/* $Id$
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
package nl.rivm.cib.episim.model.impl;

import io.coala.time.x.Duration;
import nl.rivm.cib.episim.model.Infection.SimpleInfection;
import nl.rivm.cib.episim.model.Location;
import nl.rivm.cib.episim.model.Relation;
import nl.rivm.cib.episim.model.Route;

/**
 * {@link HPV} or the Human papillomavirus has a
 * <a href="http://www.who.int/mediacentre/factsheets/fs380/en/">WHO fact
 * sheet</a>, a
 * <a href="http://emedicine.medscape.com/article/219110-overview">eMedicine
 * description</a> and a
 * <a href="http://www.diseasesdatabase.com/ddb6032.htm">DiseaseDB entry</a>,
 * from <a href="https://en.wikipedia.org/wiki/Papillomaviridae">wikipedia</a>:
 * <ul>
 * <li>&ldquo;Papillomas caused by some [HPV] types ... such as human
 * papillomaviruses 16 and 18, carry a risk of becoming cancerous.&rdquo;</li>
 * <li>&ldquo;Over 170 human papillomavirus types have been completely
 * sequenced.&rdquo;</li>
 * </ul>
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class HPV extends SimpleInfection
{

	@Override
	public boolean transmit( final Location location, final Route route,
		final Duration duration, final Relation relation )
	{
		if(Route.SEXUAL.equals( route ))
			return true;
		
		return false;
	}

}