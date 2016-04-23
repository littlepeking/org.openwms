/*
 * openwms.org, the Open Warehouse Management System.
 * Copyright (C) 2014 Heiko Scherrer
 *
 * This file is part of openwms.org.
 *
 * openwms.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * openwms.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.openwms.wms.service.spring.receiving;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openwms.common.domain.TransportUnit;
import org.openwms.common.domain.values.Barcode;
import org.openwms.common.service.TransportUnitService;
import org.openwms.core.annotation.FireAfterTransactionAsynchronous;
import org.openwms.core.domain.values.UnitType;
import org.openwms.core.service.exception.ServiceRuntimeException;
import org.openwms.core.util.validation.AssertUtils;
import org.openwms.wms.domain.LoadUnit;
import org.openwms.wms.domain.PackagingUnit;
import org.openwms.wms.domain.inventory.Product;
import org.openwms.wms.domain.order.OrderPositionKey;
import org.openwms.wms.domain.receiving.ReceivingOrder;
import org.openwms.wms.domain.receiving.ReceivingOrderCreatedEvent;
import org.openwms.wms.domain.receiving.ReceivingOrderPosition;
import org.openwms.wms.integration.LoadUnitDao;
import org.openwms.wms.integration.inventory.ProductDao;
import org.openwms.wms.integration.order.WMSOrderDao;
import org.openwms.wms.integration.receiving.ReceivingOrderDao;
import org.openwms.wms.service.receiving.Receiving;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A ReceivingManagerImpl.
 * 
 * @author <a href="mailto:scherrer@openwms.org">Heiko Scherrer</a>
 * @version $Revision: $
 * @since 0.1
 */
@Transactional
@Service(ReceivingManagerImpl.COMPONENT_NAME)
public class ReceivingManagerImpl implements Receiving {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceivingManagerImpl.class);
    @Autowired
    private ReceivingOrderDao rcvOrderDao;
    @Autowired
    private WMSOrderDao<ReceivingOrder, ReceivingOrderPosition> wmsOrderDao;
    @Autowired
    private ProductDao productDao;
    @Autowired
    private LoadUnitDao loadUnitDao;
    @Autowired
    private TransportUnitService<TransportUnit> transportUnitSrv;
    /** Springs component name. */
    public static final String COMPONENT_NAME = "receivingManager";

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ReceivingOrder> findAllOrders() {
        return rcvOrderDao.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ReceivingOrderPosition> findAllPositions(String orderId) {
        ReceivingOrder order = rcvOrderDao.findByOrderId(orderId);
        if (null == order || order.getNoPositions() == 0 || order.getPositions() == null) {
            return Collections.<ReceivingOrderPosition> emptyList();
        }
        return new ArrayList<ReceivingOrderPosition>(order.getPositions());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReceivingOrderPosition createOrderPosition(OrderPositionKey orderPositionKey, String productId,
            UnitType quantity, String barcode) {

        // Get order data
        ReceivingOrder order = rcvOrderDao.findByOrderId(orderPositionKey.getOrderId());

        // Search Product
        Product product = productDao.findByProductId(productId);

        List<LoadUnit> loadUnits = loadUnitDao.findAllOnTransportUnit(new Barcode(barcode));
        String physicalPosition = "";
        if (!loadUnits.isEmpty()) {
            String currentMaxPosition = loadUnits.get(loadUnits.size() - 1).getPhysicalPosition();
            try {
                int val = Integer.valueOf(currentMaxPosition).intValue();
                val++;
                physicalPosition = String.valueOf(val);
            } catch (NumberFormatException e) {}
        }

        TransportUnit transportUnit = transportUnitSrv.findByBarcode(new Barcode(barcode));

        LoadUnit loadUnit = new LoadUnit(transportUnit, physicalPosition, product);

        PackagingUnit pu = new PackagingUnit(loadUnit, quantity);
        ReceivingOrderPosition rcvOrderPosition = new ReceivingOrderPosition(order, orderPositionKey.getPositionNo(),
                quantity, product);
        return wmsOrderDao.createOrderPosition(rcvOrderPosition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReceivingOrder createOrder(String ordId) {
        AssertUtils.notNull(ordId, "The orderId to create an Order with is null");
        ReceivingOrder order = rcvOrderDao.findByOrderId(ordId);
        if (null != order) {
            throw new ServiceRuntimeException("An order with the id " + ordId + " already exists");
        }
        order = new ReceivingOrder(ordId);
        return wmsOrderDao.createOrder(order);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @FireAfterTransactionAsynchronous(events = { ReceivingOrderCreatedEvent.class })
    public ReceivingOrder createOrder(ReceivingOrder ord) {
        if (ord == null || !ord.isNew()) {
            throw new ServiceRuntimeException(
                    "Argument is null or the order identified by the argument already exists: " + ord);
        }
        ReceivingOrder order = rcvOrderDao.findByOrderId(ord.getOrderId());
        if (null != order) {
            throw new ServiceRuntimeException("The order " + ord + " already exists");
        }
        try {
            return wmsOrderDao.createOrder(ord);
        } finally {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ReceivingOrder created.");
            }
        }
    }
}