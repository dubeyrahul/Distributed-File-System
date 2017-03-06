package naming;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import rmi.*;
import common.*;
import storage.*;
import java.lang.System.*;

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

    // When client requests for a Storage object
    ConcurrentHashMap<Path, StorageStubs> pathToStorage = new ConcurrentHashMap<Path, StorageStubs>();

    HashSet<Path> directoriesPath;
    HashSet<Path> filesPath;

    // To compare duplicate registration
//    HashSet<Storage> storageSet = new HashSet<Storage>();
//    HashSet<Command> commandSet = new HashSet<Command>();
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
        int storageCode;
        int commandCode;

        StorageStubs(Storage s, Command c) {
            this.storage = s;
            this.command = c;
            this.storageCode = System.identityHashCode(s);
            this.commandCode = System.identityHashCode(c);
        }
        StorageStubs(){
            this.storage = null;
            this.command = null;
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

        directoriesPath = new HashSet<Path>();
        directoriesPath.add(new Path());
        filesPath = new HashSet<Path>();
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
        if(path ==null){
            throw new NullPointerException("null path");
        }
        if(path.isRoot()){
            return true;
        }
        // Need to lock path here

        if(directoriesPath.contains(path)){
            return true;
        }
        else if(filesPath.contains(path)){
            return false;
        }

        throw new FileNotFoundException("non-existent");

    }


    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
        if(directory == null) {
            throw new NullPointerException();
        }
        if(!isDirectory(directory)) {
            throw new FileNotFoundException("Path is not a directory");
        }
//        if(!pathToStorage.containsKey(directory)){
//            throw new FileNotFoundException("Path does not exist");
//        }

        String dirString = directory.toString();
//        System.out.println("Listing directory: "+dirString);
        List<Path> allPaths = new ArrayList<>(pathToStorage.keySet());
        HashSet<String> filePaths = new HashSet<String>();
        // Go through all paths
        for(Path p: allPaths){
            String temp = p.toString();
            // Find paths that contain this directory
            if(temp.contains(dirString)){
//                System.out.println("match: "+temp);
                String[] components = temp.split("/");
                if(dirString.equals("/")){
                    // Listing root directory
//                    System.out.println("Adding for root list: "+components[1]);
                    filePaths.add(components[1]);
                }
                else {
                    String newDirString = dirString.substring(1);
                    for(int i=0; i<components.length; i++){
                        if(components[i].equals(newDirString) && i+1 < components.length ){
                            filePaths.add(components[i+1]);
//                            System.out.println("Found child! "+components[i+1]);
                        }
                    }
                }

            }
        }
        return filePaths.toArray(new String[filePaths.size()]);


    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
        if(file == null){
            throw new NullPointerException("File path is null");
        }
        if(file.isRoot()){
            return false;
        }
        if(directoriesPath.contains(file) || filesPath.contains(file)){
            return false;
        }
        if(directoriesPath.contains(file.parent()) == false) {
            throw new FileNotFoundException("Parent directory non-existent");
        }

        try {
            int storageIndex = new Random().nextInt(storageServerStubsList.size());
            StorageStubs targetStorage = storageServerStubsList.get(storageIndex);
            targetStorage.command.create(file);
            filesPath.add(file);
        }
        catch(RMIException e) {
            throw new RMIException("RMI error while creating file");
        }
        return true;

    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException
    {
        if(directory == null)
            throw new NullPointerException("Directory is null");
        if(directory.isRoot()) {
            return false;
        }
        if(directoriesPath.contains(directory) || filesPath.contains(directory)){
            return false;
        }
        if(directoriesPath.contains(directory.parent()) == false){
            throw new FileNotFoundException("Parent does not exist");
        }

        // locking and unlocking needed
        directoriesPath.add(directory);
        return true;

    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {

        if(file==null){
            throw new NullPointerException("Path is invalid");
        }

        if(pathToStorage.get(file)== null){
            throw new FileNotFoundException (" No storage server for this file");
        }
        return pathToStorage.get(file).storage;
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
        for(StorageStubs stubs: storageServerStubsList){
            if(stubs.storage.hashCode() == client_stub.hashCode() && stubs.command.hashCode() == command_stub.hashCode()) {
                throw new IllegalStateException("Server is already registered");
            }
        }
        storageServerStubsList.add(storageStubs);
        /*
        Reference: http://www.codejava.net/java-core/collections/understanding-collections-and-thread-safety-in-java
        * */
//        synchronized (storageServerStubsList) {
//            if(storageServerStubsList.contains(storageStubs)){
//                throw new IllegalStateException("Server is already registered");
//            }
//            storageServerStubsList.add(storageStubs);
//        }
        // New delete
        ArrayList<Path> toDelete = new ArrayList<Path>();
        for(Path path: files) {
            if (path.isRoot())
                continue;

            int pathLen = path.components.size();
            //boolean hashFlag = false;
            boolean hashFlag = true;
            if(pathToStorage.containsKey(path)) {
//                System.out.println("Does not contain key: "+path.toString());
                hashFlag = false;

            }
            for(Path pathKey : pathToStorage.keySet()) {
//                System.out.println("Comparing Path: "+path.toString()+" and pathKey: "+pathKey.toString());
                if (pathLen < pathKey.components.size()) {
                    // checking logic

                    boolean componentFlag1 = false;
                    for (int i = 0; i < path.components.size(); i++) {

                        if (!path.components.get(i).equals(pathKey.components.get(i))) {
                            componentFlag1 = true;
                            break;
                        }

                    }
                    hashFlag = componentFlag1 && hashFlag;
                }
            }

            if(hashFlag) {
                for(Path f : files)
                {
                    if(!f.isRoot())
                    {
                        if(filesPath.contains(f)||directoriesPath.contains(f))
                        {
                            toDelete.add(f);
                        }
                        else
                        {

                            Iterator<String> i = f.iterator();
                            Path p=new Path();
                            while(i.hasNext())
                            {
                                String n=i.next();
                                if(i.hasNext())
                                {
                                    Path d=new Path(p,n);
                                    p=d;
                                    directoriesPath.add(d);
//                                    pathLocks.put(d, new PathLock());
                                }
                            }
                            filesPath.add(f);
//                            pathLocks.put(f, new PathLock());
                            pathToStorage.put(f,storageStubs);
                        }
                    }
                }

            }

//            else {
////                System.out.println("Added to delete: " + path.toString());
//                toDelete.add(path);
//            }

        }
        Path[] pathArray = toDelete.toArray(new Path[toDelete.size()]);
        return pathArray;
    }
}
