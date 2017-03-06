package common;

import java.io.*;
import java.util.*;

/**
 * Distributed filesystem paths.
 * <p>
 * <p>
 * Objects of type <code>Path</code> are used by all filesystem interfaces.
 * Path objects are immutable.
 * <p>
 * <p>
 * The string representation of paths is a forward-slash-delimeted sequence of
 * path components. The root directory is represented as a single forward
 * slash.
 * <p>
 * <p>
 * The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
 * not permitted within path components. The forward slash is the delimeter,
 * and the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Comparable<Path>, Serializable {

    public ArrayList<String> components = new ArrayList<String>();
    String fullFileName;

    /**
     * Creates a new path which represents the root directory.
     */
    public Path() {
//        components.add("");
        fullFileName = "/";

//        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Creates a new path by appending the given component to an existing path.
     *
     * @param path      The existing path.
     * @param component The new component.
     * @throws IllegalArgumentException If <code>component</code> includes the
     *                                  separator, a colon, or
     *                                  <code>component</code> is the empty
     *                                  string.
     */
    public Path(Path path, String component) {

        if (component == null || component == "" || component.contains("/") || component.contains(":"))
            throw new IllegalArgumentException("Component is not proper");


        this.components.addAll(path.components);
        this.components.add(component);
        //path.components.add(component);
//        System.out.println("Constructor 2 " + path.fullFileName);
        if(path.fullFileName.equals("/")){
            this.fullFileName = path.fullFileName + component;
        }
        else{
            this.fullFileName = path.fullFileName +"/"+ component;
        }
//        System.out.println("C2: "+this.fullFileName);


//        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Creates a new path from a path string.
     * <p>
     * <p>
     * The string is a sequence of components delimited with forward slashes.
     * Empty components are dropped. The string must begin with a forward
     * slash.
     *
     * @param path The path string.
     * @throws IllegalArgumentException If the path string does not begin with
     *                                  a forward slash, or if the path
     *                                  contains a colon character.
     */
    public Path(String path) {
        if (path == null || path == "" || path.charAt(0) != '/' || path.contains(":"))
            throw new IllegalArgumentException("String path is not proper");
        if (path.equals("/")) {
//            this.components.add("");
            this.fullFileName = "/";
        } else {
//            this.components.add("");
            String[] componentArray = path.split("/");
            for (String c : componentArray) {
                if (c.equals(""))
                    continue;
                this.components.add(c);
            }
            this.fullFileName = "";
            for(String c: this.components) {
                this.fullFileName+="/"+c;
            }
        }

    }

    /**
     * Returns an iterator over the components of the path.
     * <p>
     * <p>
     * The iterator cannot be used to modify the path object - the
     * <code>remove</code> method is not supported.
     *
     * @return The iterator.
     */
    public class PathIterator<String> implements Iterator {

        public Path currentPath;
        public  int currentIndex = -1;
        public PathIterator( Path p ){
            this.currentPath = p;
            if(p.components.size()>0)
                this.currentIndex = 0;
        }
        @Override
        public boolean hasNext() {
            if(currentIndex < currentPath.components.size())
                return true;
            return false;
        }

        @Override
        public String next() {
            if(hasNext())
                return (String)currentPath.components.get(currentIndex++);
            throw new NoSuchElementException("No more elements");
        }
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<String> iterator() {

        PathIterator<String> myIterator = new PathIterator<String>(this);
        return myIterator;
    }

    /**
     * Lists the paths of all files in a directory tree on the local
     * filesystem.
     *
     * @param directory The root directory of the directory tree.
     * @return An array of relative paths, one for each file in the directory
     * tree.
     * @throws FileNotFoundException    If the root directory does not exist.
     * @throws IllegalArgumentException If <code>directory</code> exists but
     *                                  does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException {

        File[] fileArray = directory.listFiles();
        ArrayList<Path> pathArray = new ArrayList<Path>();
        listHelper(directory, fileArray, pathArray, "/");
        Path[] result = pathArray.toArray(new Path[pathArray.size()]);
        for(Path p: result) {
//            System.out.println("Path: "+p.toString());
        }
        return result;

    }
    public static void listHelper(File directory, File[] fileArray, ArrayList<Path> pathArray, String parent) {

        for(File f: fileArray) {
            if(f.isFile()) {
                String fileName = parent+f.getName();
                pathArray.add(new Path(fileName));
            }
            else if(f.isDirectory()) {
                File[] tempFileArray = f.listFiles();
                listHelper(f, tempFileArray, pathArray, parent+f.getName()+"/");
            }
        }
    }


    /**
     * Determines whether the path represents the root directory.
     *
     * @return <code>true</code> if the path does represent the root directory,
     * and <code>false</code> if it does not.
     */
    public boolean isRoot() {

        return this.fullFileName.equals("/");
    }

    /**
     * Returns the path to the parent of this path.
     *
     * @throws IllegalArgumentException If the path represents the root
     *                                  directory, and therefore has no parent.
     */
    public Path parent() {
        Path p = null;
        if(this.isRoot()) {
            throw new IllegalArgumentException("Root has no parents");
        }
        else {
            p = new Path();
//            p.components.add("");
            for(int i=0; i<this.components.size()-1; i++) {
                if(i==0) {
                    p.fullFileName += this.components.get(i);
                }
                else {
                    p.fullFileName += "/"+this.components.get(i);
                }
                p.components.add(this.components.get(i));
            }
        }
        return p;
    }

    /**
     * Returns the last component in the path.
     *
     * @throws IllegalArgumentException If the path represents the root
     *                                  directory, and therefore has no last
     *                                  component.
     */
    public String last() {

        if(this.isRoot()){
            throw new IllegalArgumentException("Root has no last");
        }
        else {
            return this.components.get(this.components.size()-1);
        }

    }

    /**
     * Determines if the given path is a subpath of this path.
     * <p>
     * <p>
     * The other path is a subpath of this path if it is a prefix of this path.
     * Note that by this definition, each path is a subpath of itself.
     *
     * @param other The path to be tested.
     * @return <code>true</code> If and only if the other path is a subpath of
     * this path.
     */
    public boolean isSubpath(Path other) {

        return this.fullFileName.contains(other.fullFileName);
    }

    /**
     * Converts the path to <code>File</code> object.
     *
     * @param root The resulting <code>File</code> object is created relative
     *             to this directory.
     * @return The <code>File</code> object.
     */
    public File toFile(File root) {

        if(root == null) {
            // Create file with current path's filename
            return new File(this.fullFileName);
        }
        else {
            // Create file by using File root as root and append it with current path's filename
            return new File(root, this.fullFileName);
        }
    }

    /**
     * Compares this path to another.
     * <p>
     * <p>
     * An ordering upon <code>Path</code> objects is provided to prevent
     * deadlocks between applications that need to lock multiple filesystem
     * objects simultaneously. By convention, paths that need to be locked
     * simultaneously are locked in increasing order.
     * <p>
     * <p>
     * Because locking a path requires locking every component along the path,
     * the order is not arbitrary. For example, suppose the paths were ordered
     * first by length, so that <code>/etc</code> precedes
     * <code>/bin/cat</code>, which precedes <code>/etc/dfs/conf.txt</code>.
     * <p>
     * <p>
     * Now, suppose two users are running two applications, such as two
     * instances of <code>cp</code>. One needs to work with <code>/etc</code>
     * and <code>/bin/cat</code>, and the other with <code>/bin/cat</code> and
     * <code>/etc/dfs/conf.txt</code>.
     * <p>
     * <p>
     * Then, if both applications follow the convention and lock paths in
     * increasing order, the following situation can occur: the first
     * application locks <code>/etc</code>. The second application locks
     * <code>/bin/cat</code>. The first application tries to lock
     * <code>/bin/cat</code> also, but gets blocked because the second
     * application holds the lock. Now, the second application tries to lock
     * <code>/etc/dfs/conf.txt</code>, and also gets blocked, because it would
     * need to acquire the lock for <code>/etc</code> to do so. The two
     * applications are now deadlocked.
     *
     * @param other The other path.
     * @return Zero if the two paths are equal, a negative number if this path
     * precedes the other path, or a positive number if this path
     * follows the other path.
     */
    @Override
    public int compareTo(Path other) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Compares two paths for equality.
     * <p>
     * <p>
     * Two paths are equal if they share all the same components.
     *
     * @param other The other path.
     * @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other) {

        Path temp = (Path) other;
        return this.fullFileName.equals(temp.fullFileName);
    }

    /**
     * Returns the hash code of the path.
     */
    @Override
    public int hashCode() {
        return fullFileName.hashCode();
    }

    /**
     * Converts the path to a string.
     * <p>
     * <p>
     * The string may later be used as an argument to the
     * <code>Path(String)</code> constructor.
     *
     * @return The string representation of the path.
     */
    @Override
    public String toString() {
        return this.fullFileName;
    }
}
