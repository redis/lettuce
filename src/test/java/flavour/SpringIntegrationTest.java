/*
 *    Copyright 2010 The myBatis Team
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package flavour;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 
 * @version $Id$
 */
@ContextConfiguration(loader = DbTestContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class SpringIntegrationTest {

    @Autowired
    private SqlSessionFactory sessionFactory;

    @Autowired
    private WaterMapper waterMapper;

    @Autowired
    private FlavourMapper flavourMapper;

    @Test
    public void testCreateRetrieveUpdateDeleteWater() {
        SqlSession sqlSession = this.sessionFactory.openSession();

        try {
            Water water = new Water();
            water.setSize(600);
            water.setFlavourId(1);
            waterMapper.create(water);

            water = waterMapper.retrieve(water.getId());
            Assert.assertNotNull(water);
            Assert.assertTrue(water.getId() >= 1);
            Assert.assertEquals(600, water.getSize());
            Assert.assertEquals(1, water.getFlavourId());

            water.setSize(300);
            waterMapper.update(water);

            water = waterMapper.retrieve(water.getId());
            Assert.assertNotNull(water);
            Assert.assertTrue(water.getId() >= 1);
            Assert.assertEquals(300, water.getSize());
            Assert.assertEquals(1, water.getFlavourId());

            waterMapper.delete(water.getId());

            water = waterMapper.retrieve(water.getId());
            Assert.assertNull(water);
        } finally {
            sqlSession.close();
        }
    }

    @Test
    public void testCreateRetrieveUpdateDeleteFlavour() {
        SqlSession sqlSession = this.sessionFactory.openSession();

        try {
            Flavour flavour = new Flavour();
            flavour.setName("orange");
            flavourMapper.create(flavour);

            flavour = flavourMapper.retrieve(flavour.getId());
            Assert.assertNotNull(flavour);
            Assert.assertTrue(flavour.getId() >= 1);
            Assert.assertEquals("orange", flavour.getName());

            flavour.setName("wine");
            flavourMapper.update(flavour);

            flavour = flavourMapper.retrieve(flavour.getId());
            Assert.assertNotNull(flavour);
            Assert.assertTrue(flavour.getId() >= 1);
            Assert.assertEquals("wine", flavour.getName());

            flavourMapper.delete(flavour.getId());

            flavour = flavourMapper.retrieve(flavour.getId());
            Assert.assertNull(flavour);
        } finally {
            sqlSession.close();
        }
    }

    @Test
    public void testLazyLoadFlavour() {
        SqlSession sqlSession = this.sessionFactory.openSession();

        try {
            Flavour flavour = new Flavour();
            flavour.setName("orange");
            flavourMapper.create(flavour);

            Water water = new Water();
            water.setSize(600);
            water.setFlavourId(flavour.getId());
            waterMapper.create(water);

            water = waterMapper.retrieve(water.getId());
            Assert.assertNotNull(water);
            Assert.assertTrue(water.getId() >= 1);
            Assert.assertEquals(600, water.getSize());
            // To check your lazy loading setting the above output should appear BEFORE the sql log for the next line
            // if you are using the laziest version of lazy loading.
            Assert.assertEquals("orange", water.getFlavourName());

            waterMapper.delete(water.getId());
            flavourMapper.delete(flavour.getId());

            water = waterMapper.retrieve(water.getId());
            Assert.assertNull(water);
            flavour = flavourMapper.retrieve(flavour.getId());
            Assert.assertNull(flavour);
        } finally {
            sqlSession.close();
        }
    }

}
