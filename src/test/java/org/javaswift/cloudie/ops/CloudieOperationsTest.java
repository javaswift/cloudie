/*
 *  Copyright 2012-2013 E.Hooijmeijer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.javaswift.cloudie.ops;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;

import org.javaswift.cloudie.ops.CloudieOperations.CloudieCallback;
import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CloudieOperationsTest {

    private CloudieOperations ops;
    private CloudieCallback callback;
    private Account account;
    private AccountConfig accountConfig;

    @Before
    public void init() {
        accountConfig = new AccountConfig();
        accountConfig.setMockAllowEveryone(true);
        ops = new CloudieOperationsImpl(true);
        callback = Mockito.mock(CloudieCallback.class);
        //
        ops.login(accountConfig, callback);
        account = ((CloudieOperationsImpl) ops).getAccount();
    }

    @Test
    public void shouldLogin() {
        ops.login(accountConfig, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onLoginSuccess();
    }

    @Test
    public void shouldLogout() {
        ops.logout(callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onLogoutSuccess();
    }

    @Test
    public void shouldCreateContainer() {
        ops.createContainer(new ContainerSpecification("x", true), callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onUpdateContainers(Mockito.anyListOf(Container.class));
        assertFalse(account.getContainer("x").isPublic());
    }

    @Test
    public void shouldNotCreateContainer() {
        ops.createContainer(null, callback);
        Mockito.verify(callback, Mockito.never()).onUpdateContainers(Mockito.anyListOf(Container.class));
    }

    @Test
    public void shouldNotCreateContainerTwice() {
        ops.createContainer(new ContainerSpecification("x", true), callback);
        ops.createContainer(new ContainerSpecification("x", true), callback);
        Mockito.verify(callback, Mockito.atMost(2)).onUpdateContainers(Mockito.anyListOf(Container.class));
    }

    @Test
    public void shouldCreateContainerPublic() {
        ops.createContainer(new ContainerSpecification("x", false), callback);
        Mockito.verify(callback, Mockito.atMost(1)).onUpdateContainers(Mockito.anyListOf(Container.class));
        assertTrue(account.getContainer("x").isPublic());
    }

    @Test
    public void shouldCreateStoredObject() {
        ops.createStoredObjects(account.getContainer("x").create(), new File[] { new File("pom.xml") }, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();
        Mockito.verify(callback, Mockito.atLeastOnce()).onAppendStoredObjects(Mockito.any(Container.class), Mockito.eq(0),
                Mockito.anyListOf(StoredObject.class));
    }

    @Test
    public void shouldDeleteContainer() {
        ops.deleteContainer(account.getContainer("x").create(), callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onUpdateContainers(Mockito.anyListOf(Container.class));
    }

    @Test
    public void shouldDeleteStoredObject() {
        Container create = account.getContainer("x").create();
        StoredObject object = create.getObject("y");
        object.uploadObject(new byte[0]);
        ops.deleteStoredObjects(create, Collections.singletonList(object), callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onStoredObjectDeleted(Mockito.any(Container.class), Mockito.any(StoredObject.class));
    }

    @Test
    public void shouldDownloadStoredObject() {
        Container create = account.getContainer("x").create();
        StoredObject object = create.getObject("y");
        object.uploadObject(new byte[8192]);
        File target = new File("./target/downloadTest.dat");
        ops.downloadStoredObject(create, object, target, callback);
        assertTrue(target.exists());
        target.delete();
    }

    @Test
    public void shouldEmptyContainer() {
        Container create = account.getContainer("x").create();
        StoredObject object = create.getObject("y");
        object.uploadObject(new byte[8192]);
        //
        ops.emptyContainer(create, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();
    }

    @Test
    public void shouldGetMetadata() {
        Container create = account.getContainer("x").create();
        StoredObject object = create.getObject("y");
        object.uploadObject(new byte[8192]);
        //
        ops.getMetadata(create, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onContainerUpdate(create);
        //
        ops.getMetadata(object, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onStoredObjectUpdate(object);
    }

    @Test
    public void shouldPurgeContainer() {
        Container create = account.getContainer("x").create();
        StoredObject object = create.getObject("y");
        object.uploadObject(new byte[8192]);
        //
        ops.purgeContainer(create, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onUpdateContainers(Mockito.anyListOf(Container.class));
        assertFalse(create.exists());
    }

    @Test
    public void shouldRefreshContainers() {
        ops.refreshContainers(callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onUpdateContainers(Mockito.anyListOf(Container.class));
    }

    @Test
    public void shouldRefreshStoredObjects() {
        Container create = account.getContainer("x").create();
        ops.refreshStoredObjects(create, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();
    }

}
