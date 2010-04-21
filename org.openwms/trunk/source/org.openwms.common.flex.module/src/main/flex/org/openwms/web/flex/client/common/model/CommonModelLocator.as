/*
 * openwms.org, the Open Warehouse Management System.
 *
 * This file is part of openwms.org.
 *
 * openwms.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * openwms.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software. If not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.openwms.web.flex.client.common.model
{

    import com.adobe.cairngorm.model.IModelLocator;
    
    import mx.collections.ArrayCollection;
    
    /**
     * A CommonModelLocator.
     *
     * @author <a href="mailto:openwms@googlemail.com">Heiko Scherrer</a>
     * @version $Revision: 796 $
     */
    [Bindable]
    public class CommonModelLocator implements IModelLocator
    {

        public var allTransportUnitTypes:ArrayCollection = new ArrayCollection();
        private static var instance:CommonModelLocator;

        /**
         * Used to construct the Singleton instance.
         */
        public function CommonModelLocator(enforcer:SingletonEnforcer)
        {
        }

        /**
         * Return the instance of CommonModelLocator which is implemented
         * as Singleton.
         */
        public static function getInstance():CommonModelLocator
        {
            if (instance == null)
            {
                instance = new CommonModelLocator(new SingletonEnforcer);
            }
            return instance;
        }
    }
}

class SingletonEnforcer
{
}