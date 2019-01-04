package edu.rutgers.winlab.networksimulator.network.gpser.packets;

import edu.rutgers.winlab.networksimulator.common.Data;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.stream.Stream;

/**
 *
 * @author Jiachen Chen
 */
public class Name implements Data {

    public static final int NAME_SIZE = 20 * BYTE;

    private static final HashMap<Integer, Name> EXISTING_NAMES = new HashMap<>();
    private static final HashMap<Name, HashSet<Name>> NODES_DESCENDENTS = new HashMap<>();
    private static final HashMap<Name, HashSet<Name>> NODES_ANCESTORS = new HashMap<>();
    private static LinkedList<Name> TOPOLOGICAL_ORDER = null;

    public static Name getName(int representation) {
        Name ret = EXISTING_NAMES.get(representation);
        if (ret == null) {
            ret = new Name(representation);
            EXISTING_NAMES.put(representation, ret);
            if (TOPOLOGICAL_ORDER != null) {
                TOPOLOGICAL_ORDER.addLast(ret);
            }
        }
        return ret;
    }

    public static void connectNames(Name parent, Name child) {
        if (!parent.children.add(child)) {
            throw new IllegalArgumentException(String.format("parent %s already has child %s", parent, child));
        }
        child.parents.add(parent);
        TOPOLOGICAL_ORDER = null;
        NODES_DESCENDENTS.clear();
    }

    public static void disconnectNames(Name parent, Name child) {
        if (!parent.children.remove(child)) {
            throw new IllegalArgumentException(String.format("parent %s does not have child %s", parent, child));
        }
        child.parents.remove(parent);
        TOPOLOGICAL_ORDER = null;
        NODES_DESCENDENTS.clear();
    }

    public static Stream<Name> getExistingNames() {
        return EXISTING_NAMES.values().stream();
    }
    
    public static void clearExistingNames() {
        EXISTING_NAMES.clear();
        NODES_ANCESTORS.clear();
        NODES_DESCENDENTS.clear();
        TOPOLOGICAL_ORDER = null;
    }

    public static Stream<Name> topologicalSort() {
        if (TOPOLOGICAL_ORDER == null) {
            HashSet<Name> visited = new HashSet<>();
            HashSet<Name> marked = new HashSet<>();
            TOPOLOGICAL_ORDER = new LinkedList<>();
            EXISTING_NAMES.values().stream()
                    .forEach(name -> topologicalVisit(name, visited, marked));

        }
        return TOPOLOGICAL_ORDER.stream();
    }

    private static void topologicalVisit(Name start, HashSet<Name> visited, HashSet<Name> marked) {
        if (visited.contains(start)) {
            return;
        }
        if (marked.contains(start)) {
            throw new IllegalArgumentException("There is a loop in the namespace: " + Arrays.toString(marked.toArray()));
        }
        marked.add(start);
        start.children.forEach(child -> topologicalVisit(child, visited, marked));
        visited.add(start);
        marked.remove(start);
        TOPOLOGICAL_ORDER.addFirst(start);
    }

    public static Stream<Name> getNameDescendents(Name name) {
        HashSet<Name> marked = new HashSet<>();
        return getNameDescendentsVisit(name, marked).stream();
    }

    private static HashSet<Name> getNameDescendentsVisit(Name name, HashSet<Name> marked) {
        HashSet<Name> nameDescents = NODES_DESCENDENTS.get(name);
        if (nameDescents != null) {
            return nameDescents;
        }
        if (marked.contains(name)) {
            throw new IllegalArgumentException("There is a loop in the namespace: " + Arrays.toString(marked.toArray()));
        }
        marked.add(name);
        HashSet<Name> descendents = new HashSet<>();
//        descendents.add(name);
        name.children.forEach(child -> {
            descendents.addAll(getNameDescendentsVisit(child, marked));
            descendents.add(child);
        });
        NODES_DESCENDENTS.put(name, descendents);
        marked.remove(name);
        return descendents;
    }
    
    public static Stream<Name> getNameAncestors(Name name) {
        HashSet<Name> marked = new HashSet<>();
        marked.remove(name);
        return getNameAncestorsVisit(name, marked).stream();
    }
    
    private static HashSet<Name> getNameAncestorsVisit(Name name, HashSet<Name> marked) {
        HashSet<Name> nameAncestors = NODES_ANCESTORS.get(name);
        if (nameAncestors != null) {
            return nameAncestors;
        }
        if (marked.contains(name)) {
            throw new IllegalArgumentException("There is a loop in the namespace: " + Arrays.toString(marked.toArray()));
        }
        marked.add(name);
        HashSet<Name> ancestors = new HashSet<>();
//        ancestors.add(name);
        name.parents.forEach(parent->{
            ancestors.addAll(getNameAncestorsVisit(parent, marked));
            ancestors.add(parent);
        });
        NODES_ANCESTORS.put(name, ancestors);
        marked.remove(name);
        return ancestors;
    }

    private final int representation;
    private final HashSet<Name> parents = new HashSet<>(), children = new HashSet<>();

    private Name(int representation) {
        this.representation = representation;
    }

    public int getRepresentation() {
        return representation;
    }

    @Override
    public int getSizeInBits() {
        return NAME_SIZE;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.representation;
        return hash;
    }

    public Stream<Name> getParents() {
        return parents.stream();
    }

    public Stream<Name> getChildren() {
        return children.stream();
    }

    public Stream<Name> getDescendents() {
        return Name.getNameDescendents(this);
    }
    
    public Stream<Name> getAncestors() {
        return Name.getNameAncestors(this);
    }

    @Override
    public String toString() {
        return Integer.toString(representation);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Name other = (Name) obj;
        return this.representation == other.representation;
    }

}
