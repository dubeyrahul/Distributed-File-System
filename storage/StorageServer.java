package storage;

import java.io.*;
import java.net.*;

import common.*;
import rmi.*;
import naming.*;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command
{

    File root;
    SubSkeleton<Storage> storageSubSkeleton;
    SubSkeleton<Command> commandSubSkeleton;

    /* Subclass of our RMI Skeleton class to generate Storage and Command Skeleton */
    private class SubSkeleton<T> extends Skeleton<T>
    {

        StorageServer server;
        boolean isStopped = false;

        SubSkeleton(Class<T> remoteInterfaceType, T server, StorageServer s) {
            super(remoteInterfaceType, server);
            this.server = s;
        }

        SubSkeleton(Class<T> remoteInterfaceType, T server, StorageServer s,
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
    /** Creates a storage server, given a directory on the local filesystem, and
        ports to use for the client and command interfaces.

        <p>
        The ports may have to be specified if the storage server is running
        behind a firewall, and specific ports are open.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @param client_port Port to use for the client interface, or zero if the
                           system should decide the port.
        @param command_port Port to use for the command interface, or zero if
                            the system should decide the port.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root, int client_port, int command_port)
    {

        if(root == null) {
            throw new NullPointerException("Root cannot be null");
        }
        // Get the full file name of this root directory/file
        this.root = root.getAbsoluteFile();

        // Initialize Storage Skeleton for client
        if(client_port == 0) {
            storageSubSkeleton = new SubSkeleton<Storage>(Storage.class, this, this);
        }
        else {
            storageSubSkeleton = new SubSkeleton<Storage>(Storage.class, this, this,
                    new InetSocketAddress(client_port));
        }

        // Initialize Command server for Naming server
        if(command_port == 0) {
            commandSubSkeleton = new SubSkeleton<Command>(Command.class, this, this);
        }
        else {
            commandSubSkeleton = new SubSkeleton<Command>(Command.class, this, this,
                    new InetSocketAddress(command_port));
        }

    }

    /** Creats a storage server, given a directory on the local filesystem.

        <p>
        This constructor is equivalent to
        <code>StorageServer(root, 0, 0)</code>. The system picks the ports on
        which the interfaces are made available.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
     */
    public StorageServer(File root)
    {
        this(root, 0, 0);

    }

    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {
        if(root.exists()== false || root.isDirectory() == false) {
            throw new FileNotFoundException("root does not exist/ root is not a directory");
        }
        storageSubSkeleton.start();
        commandSubSkeleton.start();
        Storage stubOfStorage = Stub.create(Storage.class, storageSubSkeleton, hostname);
        Command stubOfCommand = Stub.create(Command.class, commandSubSkeleton, hostname);

    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        storageSubSkeleton.stop();
        commandSubSkeleton.stop();
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {

    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        File tempFile = file.toFile(this.root);
        if(tempFile.exists() == false) {
            throw new FileNotFoundException("File does not exist");
        }
        if(tempFile.isDirectory() == true) {
            throw new FileNotFoundException("Path directs to a directory, not a file");
        }
        return tempFile.length();
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        if(length<0){
            throw new IndexOutOfBoundsException("Length cannot be negative");
        }
        if(offset <0) {
            throw new IndexOutOfBoundsException("Offset cannot be negative");
        }

        File tempFile = file.toFile(this.root);
        if(tempFile.isDirectory()) {
            throw new FileNotFoundException("The given path belongs to a directory");
        }
        if(tempFile.exists() == false) {
            throw new FileNotFoundException("This file does not exist on the server");
        }
        if(tempFile.canRead() == false) {
            throw new FileNotFoundException("File cannot be read by the server");
        }
        if(length + offset > tempFile.length()) {
            throw new IndexOutOfBoundsException("Length + Offset exceed file length");
        }
        if(length > Integer.MAX_VALUE || offset > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("Offset and Length cannot be above int range");
        }
        /*
        Reference:
        http://tutorials.jenkov.com/java-io/randomaccessfile.html
        http://www.tutorialspoint.com/java/io/randomaccessfile_readfully_byte_len.htm
        https://examples.javacodegeeks.com/core-java/io/randomaccessfile/java-randomaccessfile-example/
        * */
        RandomAccessFile f = new RandomAccessFile(tempFile, "r");
        byte[] readArray = new byte[length];
        f.readFully(readArray,(int)offset,(int)length);
        f.close();
        return readArray;
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        File tempFile = file.toFile(root);
        if(tempFile.exists() == false) {
            throw new FileNotFoundException("File does not exist on the server");
        }
        if(tempFile.isDirectory()) {
            throw new FileNotFoundException("Path directs to a directory, not a file");
        }
        if(tempFile.canWrite() == false) {
            throw new IOException("File write cannot be completed");
        }
        if(offset < 0) {
            throw new IndexOutOfBoundsException("Offset cannot be negative");
        }
        /*
        Reference: http://tutorials.jenkov.com/java-io/randomaccessfile.html
        * */
        RandomAccessFile f = new RandomAccessFile(tempFile, "rw");
        f.seek(offset);
        f.write(data);
        f.close();
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        if(file.isRoot()) {
            // Since we cannot create a root directory
            return false;
        }
        Path parentPath = file.parent();
        File parentFile = parentPath.toFile(root);

        parentFile.mkdir();
        // Creating file by appending it to root
        File tempFile = file.toFile(root);

        /*
        Reference:
        https://docs.oracle.com/javase/7/docs/api/java/io/File.html#createNewFile()
        * */
        try {
            return tempFile.createNewFile();
        }
        catch(IOException io) {
            io.printStackTrace();
        }
        return false;
    }

    @Override
    public synchronized boolean delete(Path path)
    {

        if(path.isRoot()) {
            // Since root cannot be deleted
            return false;
        }
        File tempFile = path.toFile(root);
        if(tempFile.exists() == false) {
            // File does not exist and cannot be deleted
            return false;
        }
        if(tempFile.isFile()) {
            return tempFile.delete();
        }
        boolean deleteSuccess = deleteRecursive(tempFile);
        return deleteSuccess;
        /*
        TODO:
        Remove all the empty directories that are created after this delete operation
        * */
    }
    /*
    Reference:
    http://stackoverflow.com/questions/35745276/java-deleting-files-and-folder-of-parent-path-recursively
    * */
    private boolean deleteRecursive(File filePath) {
        // We keep deleting whatever we can because that is the requirement
        // If something cannot be deleted, it will set the success flag to false
        if(filePath.isFile()) {
            return true;
        }
        File[] list = filePath.listFiles();
        boolean success = true;
        for (File f : list) {
            if (f.isDirectory())
                success &= deleteRecursive(f);
            else {
                success &= f.delete();
            }
        }
        return success && filePath.delete();
    }

    @Override
    public synchronized boolean copy(Path file, Storage server)
        throws RMIException, FileNotFoundException, IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }
}
