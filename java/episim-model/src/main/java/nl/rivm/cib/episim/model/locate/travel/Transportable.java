/* $Id: df183d79abaee941f3131012d3302d076bbcab74 $
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
package nl.rivm.cib.episim.model.locate.travel;

import io.coala.time.Duration;
import io.coala.time.Signal;
import nl.rivm.cib.episim.model.locate.Locatable;
import nl.rivm.cib.episim.model.locate.Place;

/**
 * {@link Transportable}
 *
 * @version $Date$
 * @author Rick van Krevelen
 */
public interface Transportable extends Locatable
{

	Signal<Vehicle> vehicle();

	void moveTo( Place newSpace, Vehicle transport, Duration travelTime );

}