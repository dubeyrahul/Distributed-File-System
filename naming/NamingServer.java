package naming;

import java.io.*;
import java.net.*;
import java.util.*;

import rmi.*;
import common.*;
import storage.*;

/** Naming server.

    <p>
    Each instance of the filesystem is centered on a single naming server. The
    naming server maintains the filesystem directory tree. It does not store any
    file data - this is done by separate storage servers. The primary purpose of
    the naming server is to map each file name (path) to the storage server
    which hosts the file's contents.

    <p>
    The naming server provides two interfaces, <code>Service</code> and
    <code>Registration</code>, which are accessible through RMI. Storage servers
    use the <code>Registration</code> interface to inform the naming server of
    their existence. Clients use the <code>Service</code> interface to perform
    most filesystem operations. The documentation accompanying these interfaces
    provides details on the methods supported.

    <p>
    Stubs for accessing the naming server must typically be created by directly
    specifying the remote network address. To make this possible, the client and
    registration interfaces are available at well-known ports defined in
    <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration
{

    // service skeleton used by Clients
    SubSkeleton<Service> serviceSubSkeleton;
    // registration skeleton used by StorageServer
    SubSkeleton<Registration> registrationSubSkeleton;

    // An arraylist of storageserver stubs that this naming server knows about
    List<StorageStubs> storageServerStubsList = Collections.synchronizedList(new ArrayList<>());

    /* Subclass of our RMI Skeleton class to generate Service and Registration Skeleton */
    private class SubSkeleton<T> extends Skeleton<T>
    {

        NamingServer server;
        boolean isStopped = false;

        SubSkeleton(Class<T> remoteInterfaceType, T server, NamingServer s) {
            super(remoteInterfaceType, server);
            this.server = s;
        }

        SubSkeleton(Class<T> remoteInterfaceType, T server, NamingServer s,
                    InetSocketAddress address) {
            super(remoteInterfaceType, server, address);
            this.server = s;
        }
        @Override
        protected synchronized void stopped(Throwable cause)
        {
            this.isStopped = true;
        }
    }

    public class StorageStubs {
        Storage storage;
        Command command;

        StorageStubs(Storage s, Command c) {
            this.storage = s;
            this.command = c;
        }
    }
    /** Creates the naming server object.

        <p>
        The naming server is not started.
     */
    public NamingServer()
    {

        // Create service skeleton with service port mentioned in the NamingStubs file
        serviceSubSkeleton = new SubSkeleton<Service>(Service.class, this, this,
                new InetSocketAddress(NamingStubs.SERVICE_PORT));

        registrationSubSkeleton = new SubSkeleton<Registration>(Registration.class, this, this,
                new InetSocketAddress(NamingStubs.REGISTRATION_PORT));
    }

    /** Starts the naming server.

        <p>
        After this method is called, it is possible to access the client and
        registration interfaces of the naming server remotely.

        @throws RMIException If either of the two skeletons, for the client or
                             registration server interfaces, could not be
                             started. The user should not attempt to start the
                             server again if an exception occurs.
     */
    public synchronized void start() throws RMIException
    {
        serviceSubSkeleton.start();
        registrationSubSkeleton.start();
    }

    /** Stops the naming server.

        <p>
        This method commands both the client and registration interface
        skeletons to stop. It attempts to interrupt as many of the threads that
        are executing naming server code as possible. After this method is
        called, the naming server is no longer accessible remotely. The naming
        server should not be restarted.
     */
    public void stop()
    {
        serviceSubSkeleton.stop();
        registrationSubSkeleton.stop();
        this.stopped(new Throwable("Stop called on naming server"));
    }

    /** Indicates that the server has completely shut down.

        <p>
        This method should be overridden for error reporting and application
        exit purposes. The default implementation does nothing.

        @param cause The cause for the shutdown, or <code>null</code> if the
                     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following public methods are documented in Service.java.
    @Override
    public void lock(Path path, boolean exclusive) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void unlock(Path path, boolean exclusive)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files)
    {
        if(client_stub == null || command_stub == null || files == null) {
            throw new NullPointerException("None of the arguments can be null");
        }
        StorageStubs storageStubs = new StorageStubs(client_stub, command_stub);
        /*
        Reference: http://www.codejava.net/java-core/collections/understanding-collections-and-thread-safety-in-java
        * */
        synchronized (storageServerStubsList) {
            if(storageServerStubsList.contains(storageStubs)){
                throw new IllegalStateException("Server is already registered");
            }
            storageServerStubsList.add(storageStubs);
        }
        Path[] pathArray = new Path[]{};
        return pathArray;
    }
}