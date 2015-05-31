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
import java.util.Collection;
import java.util.List;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;

/**
 * CloudieOperations, these are operations the user interface can perform. All
 * methods take a CloudieCallback as last argument, which will be called during
 * the (asynchronous) execution of the operation.
 * 
 * @author E.Hooijmeijer
 */
public interface CloudieOperations {

    /**
     * callback interface, CloudieOperations will use this to notify the user
     * interface of updates.
     */
    public interface CloudieCallback {

        /**
         * signals the start of an operation.
         */
        void onStart();

        /**
         * signals the end of an operation. Its always called if onStart() was
         * called first.
         */
        void onDone();

        /**
         * signals an protocol exception.
         * @param ex the exception.
         */
        void onError(CommandException ex);

        /**
         * signals an update of the available containers.
         * @param containers the containers.
         */
        void onUpdateContainers(Collection<Container> containers);

        /**
         * signals an updated of the available stored objects.
         * @param storedObjects the containers.
         */
        // void onUpdateStoredObjects(Collection<StoredObject> storedObjects);

        /**
         * called when a new list of objects starts. followed by at least one
         * call to onAppendStoredObjects.
         */
        void onNewStoredObjects();

        /**
         * appends newly loaded objects to the list.
         * @param container the container to which the objects belong.
         * @param page the page.
         * @param storedObjects the objects.
         */
        void onAppendStoredObjects(Container container, int page, Collection<StoredObject> storedObjects);

        /**
         * signals a successful authentication.
         */
        void onLoginSuccess();

        /**
         * signals a successful logout.
         */
        void onLogoutSuccess();

        /**
         * signals the update of a containers metadata.
         * @param container the container.
         */
        void onContainerUpdate(Container container);

        /**
         * signals the update of a stored objects meta data.
         * @param obj the stored object.
         */
        void onStoredObjectUpdate(StoredObject obj);

        /**
         * called when the number of calls have changed.
         */
        void onNumberOfCalls(int nrOfCalls);

        /**
         * @param storedObject the deleted stored object.
         */
        void onStoredObjectDeleted(Container container, StoredObject storedObject);

    }

    /**
     * initializes the connection and performs a login.
     * @param callback the callback.
     */
    void login(AccountConfig accountConfig, CloudieCallback callback);

    /**
     * logout from the current session
     * @param callback the callback.
     */
    void logout(CloudieCallback callback);

    /**
     * creates a new container.
     * @param spec the container specifications.
     * @param callback the callback to call when done.
     */
    void createContainer(ContainerSpecification spec, CloudieCallback callback);

    /**
     * creates a new stored objects.
     * @param container the container to store in.
     * @param file the file(s) to upload.
     * @param callback the callback to call when done.
     */
    void createStoredObjects(Container container, File[] file, CloudieCallback callback);

    /**
     * deletes a container and all files in it.
     * @param container the container.
     * @param callback the callback to call when done.
     */
    void deleteContainer(Container container, CloudieCallback callback);

    /**
     * deletes a single stored object.
     * @param container the container holding the object.
     * @param storedObject the object.
     * @param callback the callback to call.
     */
    void deleteStoredObjects(Container container, List<StoredObject> storedObject, CloudieCallback callback);

    /**
     * downloads a stored object into a file.
     * @param container the container.
     * @param storedObject the stored object to download.
     * @param target the target file.
     * @param callback the callback to call when done.
     */
    void downloadStoredObject(Container container, StoredObject storedObject, File target, CloudieCallback callback);

    /**
     * purges a container, deleting all files and the container.
     * @param container the container
     * @param callback the callback to call when done.
     */
    void purgeContainer(Container container, CloudieCallback callback);

    /**
     * empties a container, deleting all files but not the container.
     * @param c the container
     * @param callback the callback to call when done.
     */
    void emptyContainer(Container c, CloudieCallback callback);

    /**
     * refreshes the container list.
     * @param callback the callback to call when done.
     */
    void refreshContainers(CloudieCallback callback);

    /**
     * refreshes the stored object list in the given container.
     * @param container the container.
     * @param callback the callback to call when done.
     */
    void refreshStoredObjects(Container container, CloudieCallback callback);

    /**
     * retrieves metadata for the given container.
     * @param c the container.
     * @param callback the callback when done.
     */
    void getMetadata(Container c, CloudieCallback callback);

    /**
     * retrieves metadata for the given stored object.
     * @param obj the object.
     * @param callback the callback to call when done.
     */
    void getMetadata(StoredObject obj, CloudieCallback callback);

}
