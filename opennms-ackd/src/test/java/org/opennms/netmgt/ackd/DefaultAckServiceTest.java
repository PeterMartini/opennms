/*
 * This file is part of the OpenNMS(R) Application.
 *
 * OpenNMS(R) is Copyright (C) 2009 The OpenNMS Group, Inc.  All rights reserved.
 * OpenNMS(R) is a derivative work, containing both original code, included code and modified
 * code that was published under the GNU General Public License. Copyrights for modified
 * and included code are below.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * Modifications:
 * 
 * Created: January 26, 2009
 *
 * Copyright (C) 2009 The OpenNMS Group, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * For more information contact:
 *      OpenNMS Licensing       <license@opennms.org>
 *      http://www.opennms.org/
 *      http://www.opennms.com/
 */
package org.opennms.netmgt.ackd;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.dao.AcknowledgmentDao;
import org.opennms.netmgt.dao.DatabasePopulator;
import org.opennms.netmgt.dao.EventDao;
import org.opennms.netmgt.dao.NodeDao;
import org.opennms.netmgt.dao.NotificationDao;
import org.opennms.netmgt.dao.db.JUnitTemporaryDatabase;
import org.opennms.netmgt.dao.db.OpenNMSConfigurationExecutionListener;
import org.opennms.netmgt.dao.db.TemporaryDatabaseExecutionListener;
import org.opennms.netmgt.dao.support.DefaultAckService;
import org.opennms.netmgt.model.AckType;
import org.opennms.netmgt.model.Acknowledgeable;
import org.opennms.netmgt.model.OnmsAcknowledgment;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsNotification;
import org.opennms.netmgt.model.OnmsUserNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({
    OpenNMSConfigurationExecutionListener.class,
    TemporaryDatabaseExecutionListener.class,
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class
})
@ContextConfiguration(locations={
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-daemon.xml",
        "classpath:/META-INF/opennms/mockEventIpcManager.xml",
        "classpath:/META-INF/opennms/applicationContext-setupIpLike-enabled.xml",
        "classpath:ackdTest.xml",
        "classpath:/META-INF/opennms/applicationContext-databasePopulator.xml" })

/**
 * Test class.
 */
@JUnitTemporaryDatabase(populate=true) @Transactional
public class DefaultAckServiceTest {

    @Autowired DefaultAckService m_ackService;
    
    @Autowired AcknowledgmentDao m_ackDao;

    @Autowired NotificationDao m_notifDao;
    
    @Autowired EventDao m_eventDao;
    
    @Autowired NodeDao m_nodeDao;

    @Autowired DatabasePopulator m_populator;
    
    @Before public void createDb() {
        Properties props = new Properties();
        props.setProperty("log4j.logger.org.hibernate", "INFO");
        props.setProperty("log4j.logger.org.springframework", "INFO");
        props.setProperty("log4j.logger.org.hibernate.SQL", "DEBUG");
        m_populator.populateDatabase();
    }
    
    @Test public void verifyWiring() {
        Assert.assertNotNull(m_ackService);
        Assert.assertNotNull(m_ackDao);
        Assert.assertNotNull(m_notifDao);
        Assert.assertNotNull(m_eventDao);
        Assert.assertNotNull(m_nodeDao);
        Assert.assertNotNull(m_populator);
    }
    
    @Transactional @Test 
    public void proccessAck() {
        
        OnmsNode dbNode = m_nodeDao.get(Integer.valueOf(1));

        OnmsEvent event = new OnmsEvent();
        event.setDistPoller(dbNode.getDistPoller());
        event.setEventUei(EventConstants.NODE_DOWN_EVENT_UEI);
        event.setEventTime(new Date());
        event.setEventSource("test");
        event.setEventCreateTime(new Date());
        event.setEventSeverity(1);
        event.setEventLog("Y");
        event.setEventDisplay("Y");
        event.setNode(dbNode);
        m_eventDao.save(event);
        
        OnmsNotification notif = new OnmsNotification();
        notif.setEvent(event);
        notif.setNode(dbNode);
        notif.setNumericMsg("123456");
        notif.setPageTime(Calendar.getInstance().getTime());
        notif.setSubject("ackd notif test");
        notif.setTextMsg("ackd notif test");
        
        OnmsUserNotification un = new OnmsUserNotification();
        un.setUserId("admin");
        un.setNotification(notif);
        Set<OnmsUserNotification> usersNotified = new HashSet<OnmsUserNotification>();
        usersNotified.add(un);
        notif.setUsersNotified(usersNotified);
        m_notifDao.save(notif);
        m_notifDao.flush();

        Assert.assertTrue(m_notifDao.countAll() > 0);
        
        List<OnmsNotification> notifs = m_notifDao.findAll();
        Assert.assertTrue((notifs.contains(notif)));
        
        OnmsAcknowledgment ack = new OnmsAcknowledgment();
        ack.setRefId(notif.getNotifyId());
        ack.setAckType(AckType.NOTIFICATION);
        m_ackService.processAck(ack);
        
        List<Acknowledgeable> ackables = m_ackDao.findAcknowledgables(ack);
        Assert.assertEquals(1, ackables.size());
        Acknowledgeable ackable = ackables.get(0);
        Assert.assertEquals("admin", ackable.getAckUser());
        
    }

}
