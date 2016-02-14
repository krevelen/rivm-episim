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
package nl.rivm.cib.episim.model;

import io.coala.time.x.Instant;

/**
 * {@link TransmissionEvent} generated by some {@link Infection} represents its
 * successful invasion of a susceptible {@link Carrier} that was exposed due to
 * some {@link ContactEvent}.
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public abstract class TransmissionEvent
{
	/** @return the transmission {@link Instant} */
	public abstract Instant getTime();

	/** @return the transmission {@link Location} */
	public abstract Location getLocation();

	/** @return the transmission {@link Route} */
	public abstract Route getRoute();

	/** @return the transmitted {@link Infection} */
	public abstract Infection getInfection();

	/** @return the primary infective {@link Carrier} */
	public abstract Carrier getPrimaryInfective();

	/** @return the secondary exposed {@link Carrier} */
	public abstract Carrier getSecondaryExposed();

	public static TransmissionEvent valueOf( final ContactEvent contact,
		final Instant time, final Infection infection )
	{
		return new TransmissionEvent()
		{
			@Override
			public Instant getTime()
			{
				return time;
			}

			@Override
			public Location getLocation()
			{
				return contact.getLocation();
			}

			@Override
			public Route getRoute()
			{
				return contact.getRoute();
			}

			@Override
			public Infection getInfection()
			{
				return infection;
			}

			@Override
			public Carrier getPrimaryInfective()
			{
				return contact.getPrimaryInfective();
			}

			@Override
			public Carrier getSecondaryExposed()
			{
				return contact.getSecondarySusceptible();
			}
		};
	}
}