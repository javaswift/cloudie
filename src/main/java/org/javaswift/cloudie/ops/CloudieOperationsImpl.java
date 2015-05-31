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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.impl.ClientImpl;
import org.javaswift.joss.client.mock.ClientMock;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Client;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.PaginationMap;
import org.javaswift.joss.model.StoredObject;

/**
 * CloudyOperationsImpl.
 * 
 * @author E.Hooijmeijer
 * 
 */
public class CloudieOperationsImpl implements CloudieOperations {

    private static final int MAX_PAGE_SIZE = 9999;
    private Client<?> client;
    private Account account;
    private boolean mock;

    public CloudieOperationsImpl() {
        this(false);
    }

    public CloudieOperationsImpl(boolean mock) {
        this.mock = mock;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void login(AccountConfig config, CloudieCallback callback) {
        //
        if (account != null) {
            logout(callback);
        }
        //
        client = mock ? new ClientMock(config) : new ClientImpl(config);
        account = client.authenticate();
        callback.onLoginSuccess();
        callback.onNumberOfCalls(account.getNumberOfCalls());
        //
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void logout(CloudieCallback callback) {
        account = null;
        callback.onLogoutSuccess();
        callback.onNumberOfCalls(0);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void createContainer(ContainerSpecification spec, CloudieCallback callback) {
        if (spec != null) {
            Container c = account.getContainer(spec.getName());
            if (!c.exists()) {
                c.create();
                if (spec.isPrivateContainer()) {
                    c.makePrivate();
                } else {
                    c.makePublic();
                }
            }
            callback.onUpdateContainers(eagerFetchContainers(account));
            callback.onNumberOfCalls(account.getNumberOfCalls());
        }
    }

    private Collection<Container> eagerFetchContainers(Account parent) {
        List<Container> results = new ArrayList<Container>(parent.getCount());
        PaginationMap map = parent.getPaginationMap(MAX_PAGE_SIZE);
        for (int page = 0; page < map.getNumberOfPages(); page++) {
            results.addAll(parent.list(map, page));
        }
        return results;
    }

    private Collection<StoredObject> eagerFetchStoredObjects(Container parent) {
        List<StoredObject> results = new ArrayList<StoredObject>(parent.getCount());
        PaginationMap map = parent.getPaginationMap(MAX_PAGE_SIZE);
        for (int page = 0; page < map.getNumberOfPages(); page++) {
            results.addAll(parent.list(map, page));
        }
        return results;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void createStoredObjects(Container container, File[] selectedFiles, CloudieCallback callback) {
        for (File selected : selectedFiles) {
            if (selected.isFile() && selected.exists()) {
                StoredObject obj = container.getObject(selected.getName());
                obj.uploadObject(selected);
            }
        }
        reloadContainer(container, callback);
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void deleteContainer(Container container, CloudieCallback callback) {
        container.delete();
        callback.onUpdateContainers(eagerFetchContainers(account));
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void deleteStoredObjects(Container container, List<StoredObject> storedObjects, CloudieCallback callback) {
        for (StoredObject storedObject : storedObjects) {
            storedObject.delete();
            callback.onStoredObjectDeleted(container, storedObject);
        }
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void downloadStoredObject(Container container, StoredObject storedObject, File target, CloudieCallback callback) {
        storedObject.downloadObject(target);
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void emptyContainer(Container container, CloudieCallback callback) {
        for (StoredObject so : eagerFetchStoredObjects(container)) {
            so.delete();
            callback.onNumberOfCalls(account.getNumberOfCalls());
        }
        reloadContainer(container, callback);
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void purgeContainer(Container container, CloudieCallback callback) {
        for (StoredObject so : eagerFetchStoredObjects(container)) {
            so.delete();
            callback.onNumberOfCalls(account.getNumberOfCalls());
        }
        container.delete();
        callback.onUpdateContainers(eagerFetchContainers(account));
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void refreshContainers(CloudieCallback callback) {
        callback.onUpdateContainers(eagerFetchContainers(account));
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void refreshStoredObjects(Container container, CloudieCallback callback) {
        reloadContainer(container, callback);
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    private void reloadContainer(Container container, CloudieCallback callback) {
        int page = 0;
        List<StoredObject> list = (List<StoredObject>) container.list("", null, MAX_PAGE_SIZE);
        callback.onNewStoredObjects();
        while (!list.isEmpty()) {
            callback.onAppendStoredObjects(container, page++, list);
            list = (List<StoredObject>) container.list("", list.get(list.size() - 1).getName(), MAX_PAGE_SIZE);
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void getMetadata(Container c, CloudieCallback callback) {
        c.getMetadata();
        callback.onContainerUpdate(c);
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void getMetadata(StoredObject obj, CloudieCallback callback) {
        obj.getMetadata();
        callback.onStoredObjectUpdate(obj);
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    /**
     * @return the account (for testing only!).
     */
    protected Account getAccount() {
        return account;
    }

}
