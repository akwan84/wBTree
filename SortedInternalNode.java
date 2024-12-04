import java.util.*;

public class SortedInternalNode {

    public static final int MAX = 10;

    List<Integer> keys;

    List<SortedInternalNode> internalChildren;
    List<LeafNode> leafChildren;

    public SortedInternalNode(){
        keys = new ArrayList<>();
        internalChildren = new ArrayList<>();
        leafChildren = new ArrayList<>();
    }

    public void insert(int key) {
        int i = keys.size() - 1;

        int reads = 0;
        while(i >= 0 && keys.get(i) > key) { //1 read per iteration
            i--;
            reads++;
        }
        i++;

        //simulating number of reads of a binary search (log_2(reads))
        if(reads > 0) {
            SWbTree.numReads += (int)Math.ceil((Math.log((double)reads) / Math.log(2.0))) + 1;
        }

        if(leafChildren.size() != 0) {
            if(leafChildren.get(i).keys.size() == MAX) { //1 read
                leafChildren.get(i).split(this); //1 read
                
                i = keys.size() - 1;

                reads = 0;
                while(i >= 0 && keys.get(i) > key) { //1 read per iteration
                    i--;
                }

                //simulating a binary search which could have been done above and the 2 reads above
                if(reads > 0) {
                    SWbTree.numReads += (int)Math.ceil((Math.log((double)reads) / Math.log(2.0))) + 3;
                }

                i++;
            }
            leafChildren.get(i).insert(key);
        }else{
            if(internalChildren.get(i).keys.size() == MAX) { //1 read
                internalChildren.get(i).split(this); //1 read
                
                i = keys.size() - 1;

                reads = 0;
                while(i >= 0 && keys.get(i) > key) { //1 read per iteration
                    i--;
                    reads++;
                }

                //simulating a binary search which could have been done above and the 2 reads above
                if(reads > 0) {
                    SWbTree.numReads += (int)Math.ceil((Math.log((double)reads) / Math.log(2.0))) + 3;
                }else{
                    SWbTree.numReads++;
                }
                i++;
            }
            internalChildren.get(i).insert(key);
        }
    }

    public void split(SortedInternalNode parent) {
        //create the new node
        SortedInternalNode newNode = new SortedInternalNode(); //assuming this is being created and stored in NVM

        int promote = keys.get(MAX/2);

        //put the larger half of the keys into the new node
        for(int i = MAX/2 + 1; i < MAX; i++) { //1 write per iteration
            newNode.keys.add(keys.get(i));
            SWbTree.numWrites++;
        }

        //restructure the smaller half of the keys
        List<Integer> newKeys = new ArrayList<>();

        for(int i = 0; i < MAX/2; i++) {
            newKeys.add(keys.get(i));
        }

        keys = newKeys;

        //split the pointers
        if(leafChildren.size() != 0) { //this internal node had leaf children
            //put the larger half of the children into the new node
            for(int i = (int)Math.ceil((double)(MAX + 1)/2); i < MAX + 1; i++) {
                newNode.leafChildren.add(leafChildren.get(i)); //1 write
                SWbTree.numWrites++;
            }

            //restructure the smaller half of the keys
            List<LeafNode> newChildren = new ArrayList<>();

            for(int i = 0; i < (int)Math.ceil((double)(MAX + 1)/2); i++) {
                newChildren.add(leafChildren.get(i));
            }

            leafChildren = newChildren;
        } else { //internal node had internal children
            //put the larger half of the children into the new node
            for(int i = (int)Math.ceil((double)(MAX + 1)/2); i < MAX + 1; i++) {
                newNode.internalChildren.add(internalChildren.get(i)); //1 write
                SWbTree.numWrites++;
            }

            //restructure the smaller half of the keys
            List<SortedInternalNode> newChildren = new ArrayList<>();

            for(int i = 0; i < (int)Math.ceil((double)(MAX + 1)/2); i++) {
                newChildren.add(internalChildren.get(i));
            }

            internalChildren = newChildren;
        }

        //key promotion
        if(parent.keys.size() == 0) { //parent is a brand new internal node
            parent.keys.add(promote);

            parent.internalChildren.add(this); //1 write
            parent.internalChildren.add(newNode); //1 write
            SWbTree.numWrites += 2;
        } else { //parent already has this node, need to find the right insertion point for newNode

            int l = 0, r = parent.keys.size();
            while (l < r) {
                int mid = (l + r) / 2;
                if (parent.keys.get(mid) < promote) { //1 read
                    l = mid + 1;
                } else {
                    r = mid; // Potential insertion point
                }
                SWbTree.numReads++;
            }

            parent.keys.add(l, promote); //1 write
            SWbTree.numWrites++;

            
            //find the right spot for newNode in the parent
            l = 0; 
            r = parent.internalChildren.size();
            while (l < r) {
                int mid = (l + r) / 2;
                SortedInternalNode searchNode = parent.internalChildren.get(mid); //1 read
                if (searchNode.keys.get(searchNode.keys.size() - 1) < promote) {
                    l = mid + 1;
                } else {
                    r = mid; // Potential insertion point
                }
                SWbTree.numReads++;
            }

            parent.internalChildren.add(l, newNode); //1 write
            SWbTree.numWrites++;
        }
    }
}
