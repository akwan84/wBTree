import java.util.*;

class SWbTree {
    public static int numReads = 0;
    public static int numWrites = 0;

    LeafNode oldRoot;
    SortedInternalNode root;
    int numLevels;
    String lastState;

    public static final int MAX = 10;

    public SWbTree() {
        oldRoot = new LeafNode();
        root = new SortedInternalNode();
        numLevels = 1;
        lastState = "";
    }

    public boolean search(int key) {
        if(numLevels == 1) {
            return searchLeaf(key, oldRoot);
        }else{
            SortedInternalNode cur = root;
            LeafNode target = new LeafNode();
            for(int i = 0; i < numLevels - 1; i++) {
                int index = cur.keys.size() - 1;
                while(index >= 0 && key < cur.keys.get(index)) {
                    index--;
                }

                if(cur.leafChildren.size() != 0) {
                    target = cur.leafChildren.get(index + 1); //1 read
                }else{
                    cur = cur.internalChildren.get(index + 1); //1 read
                }
                SWbTree.numReads++;
            }
            return searchLeaf(key, target);
        }
    }

    private boolean searchLeaf(int key, LeafNode node) {
        int l = 0;
        int r = node.keys.size() - 1;

        while(l <= r) {
            int mid = (l + r) / 2;
            if(node.keys.get(node.slotArr.get(mid)) == key) {
                SWbTree.numReads++;
                return true; 
            }
            SWbTree.numReads++;

            if(node.keys.get(node.slotArr.get(mid)) < key) { //1 read
                l = mid + 1;
            }else{
                r = mid - 1;
            }
            SWbTree.numReads++;
        }
        SWbTree.numReads++;
        return false;
    }

    public void insert(int key) {
        if(numLevels == 1 && oldRoot.keys.size() < MAX) {
            oldRoot.insert(key);
        } else {
            if(numLevels == 1) {
                oldRoot.split(root);
                numLevels++;
            } else if (root.keys.size() == MAX){
                SortedInternalNode newRoot = new SortedInternalNode();
                root.split(newRoot);
                root = newRoot;
                numLevels++;
            }
            root.insert(key);
        }

        lastState = serialize();
    }

    public void printTree() {
        Queue<SortedInternalNode> queue = new LinkedList<>();
        Queue<LeafNode> queue2 = new LinkedList<>();

        queue.add(root);

        while(!queue.isEmpty()) {
            for(int j = queue.size(); j > 0; j--) {
                SortedInternalNode cur = queue.poll();

                if(cur.leafChildren.size() == 0) {
                    for(int i = 0; i < cur.internalChildren.size(); i++) {
                        queue.add(cur.internalChildren.get(i));
                    }
                }else{
                    for(int i = 0; i < cur.leafChildren.size(); i++) {
                        queue2.add(cur.leafChildren.get(i));
                    }
                }

                for(int i = 0; i < cur.keys.size(); i++) {
                    System.out.print(cur.keys.get(i) + " ");
                }
                System.out.print("      ");
            }
            System.out.println();
        }

        while(!queue2.isEmpty()) {
            LeafNode cur = queue2.poll();

            for(int i = 0; i < cur.keys.size(); i++) {
                System.out.print(cur.keys.get(cur.slotArr.get(i)) + " ");
            }
            System.out.print("      ");
        }
        System.out.println();
    }

    private String serialize() {
        StringBuilder str = new StringBuilder();
        if(numLevels == 1) {
            for(int i = 0; i < oldRoot.keys.size(); i++) {
                str.append(oldRoot.keys.get(oldRoot.slotArr.get(i)));
                str.append(",");
            }
            return str.toString().substring(0, str.length() - 1);
        }else{
            Queue<SortedInternalNode> queue = new LinkedList<>();
            Queue<LeafNode> queue2 = new LinkedList<>();

            queue.add(root);

            while(!queue.isEmpty()) {
                for(int j = queue.size(); j > 0; j--) {
                    SortedInternalNode cur = queue.poll();

                    if(cur.leafChildren.size() == 0) {
                        for(int i = 0; i < cur.internalChildren.size(); i++) {
                            queue.add(cur.internalChildren.get(i));
                        }
                    }else{
                        for(int i = 0; i < cur.leafChildren.size(); i++) {
                            queue2.add(cur.leafChildren.get(i));
                        }
                    }
                }
            }

            while(!queue2.isEmpty()) {
                LeafNode cur = queue2.poll();

                for(int i = 0; i < cur.keys.size(); i++) {
                    str.append(cur.keys.get(cur.slotArr.get(i)));
                    str.append(",");
                }
            }
            return str.toString().substring(0, str.length() - 1);
        }
    }
    
    public void rebuild() {
        List<Integer> keys = new ArrayList<>();
        
        /* Parse the last state of the tree to get the keys to rebuild the tree with */
        StringBuilder curNum = new StringBuilder();
        for(char c : lastState.toCharArray()) {
            if(c != ',') {
                curNum.append(c);
            }else{
                keys.add(Integer.parseInt(curNum.toString()));
                curNum = new StringBuilder();
            }
        }

        keys.add(Integer.parseInt(curNum.toString()));



        /* Rebuild Leaf Nodes */
        List<LeafNode> leaves = new ArrayList<>();
        LeafNode cur = new LeafNode(); //assume this is created in NVM

        for(int key : keys) {
            if(cur.keys.size() >= MAX/2) { //fill every leaf node to 50% capacity
                leaves.add(cur);
                cur = new LeafNode();
            }

            cur.keys.add(key); //1 write
            cur.slotArr.add(cur.keys.size() - 1);
            SWbTree.numWrites++;
        }
        leaves.add(cur);

        //Small enough that the tree only consists of 1 leaf node
        if(leaves.size() == 1) {
            oldRoot = leaves.get(0);
            numLevels = 1;
            return;
        }



        /* Build first level of internal nodes from leaf nodes */
        List<SortedInternalNode> newInternals = new ArrayList<>();
        SortedInternalNode curInternal = new SortedInternalNode();
        for(LeafNode leaf : leaves) {
            if(curInternal.leafChildren.size() == 0) { //1 read
                curInternal.leafChildren.add(leaf); //1 write
                SWbTree.numWrites++;
            }else{
                if(curInternal.keys.size() >= MAX/2) { //1 read
                    newInternals.add(curInternal);
                    curInternal = new SortedInternalNode();
                    
                    curInternal.leafChildren.add(leaf); //1 write
                    SWbTree.numWrites++;
                } else {
                    curInternal.leafChildren.add(leaf); //1 write
                    curInternal.keys.add(leaf.keys.get(0)); //1 write
                    SWbTree.numWrites++; 
                }
            }
            SWbTree.numReads++;
        }

        if(curInternal.leafChildren.size() == 1) {
            //can not have an internal node with 1 child, move it over to the previous node
            LeafNode n = curInternal.leafChildren.get(0); //1 read

            SortedInternalNode last = newInternals.get(newInternals.size() - 1); //1 read
            
            last.leafChildren.add(n);  //1 write
            last.keys.add(n.keys.get(0)); //1 write

            SWbTree.numReads += 2;
            SWbTree.numWrites += 2;
        }else{
            newInternals.add(curInternal);
        }

        
        
        /* Build the rest of the internal nodes until only 1 remains */
        int level = 2;
        while(newInternals.size() > 1) {
            List<SortedInternalNode> nextLevel = new ArrayList<>();
            curInternal = new SortedInternalNode();

            for(int i = 0; i < newInternals.size(); i++) {
                if(curInternal.internalChildren.size() == 0) {
                    curInternal.internalChildren.add(newInternals.get(i));
                } else {
                    if(curInternal.keys.size() >= MAX/2) {
                        nextLevel.add(curInternal);
                        curInternal = new SortedInternalNode();

                        curInternal.internalChildren.add(newInternals.get(i)); //1 write

                        SWbTree.numWrites++;
                    } else {
                        SortedInternalNode last = curInternal.internalChildren.get(curInternal.internalChildren.size() - 1);  //1 read
                        int lastKey = last.keys.get(last.keys.size() - 1); //1 read
                        int firstKey = newInternals.get(i).keys.get(0); //1 read

                        int mid = (int)Math.ceil(((double)(firstKey + lastKey)) / 2.0);

                        curInternal.internalChildren.add(newInternals.get(i)); //1 write

                        curInternal.keys.add(mid); //1 write

                        SWbTree.numReads += 3;
                        SWbTree.numWrites += 2;
                    }
                }
            }

            if(curInternal.internalChildren.size() == 1) {
                SortedInternalNode lastNextLevel = nextLevel.get(nextLevel.size() - 1);
                SortedInternalNode n = curInternal.internalChildren.get(0); //1 read
                SortedInternalNode adjChild = lastNextLevel.internalChildren.get(lastNextLevel.internalChildren.size() - 1); //1 read

                int lastKey = adjChild.keys.get(adjChild.keys.size() - 1); //1 read
                int firstKey = n.keys.get(0); //1 read

                int mid = (int)Math.ceil(((double)(firstKey + lastKey)) / 2.0);

                lastNextLevel.internalChildren.add(n); //1 write

                lastNextLevel.keys.add(mid); //1 write

                SWbTree.numReads += 4;
                SWbTree.numWrites += 2;
            }else{
                nextLevel.add(curInternal);
            }
            level++;
            newInternals = nextLevel;
        }

        root = newInternals.get(0);
        numLevels = level;
        lastState = serialize();
    }

    public static void resetStats() {
        SWbTree.numReads = 0;
        SWbTree.numWrites = 0;
        WbTree.numReads = 0;
        WbTree.numWrites = 0;
    }

    public static void main(String[] args) {
        /* Creating trees with 100, 1000, 10000, and 50000 keys */
        System.out.println("Read and Writes in Insertion into swB+ Tree");
        SWbTree tree1 = new SWbTree();

        resetStats();
        for(int i = 0; i < 100; i++) {
            tree1.insert(i);
        }

        System.out.println("Number of Reads with 100 Keys: " + SWbTree.numReads);
        System.out.println("Number of Writes with 100 Keys: " + SWbTree.numWrites);


        resetStats();
        SWbTree tree2 = new SWbTree();
        System.out.println();


        for(int i = 0; i < 1000; i++) {
            tree2.insert(i);
        }

        System.out.println("Number of Reads with 1000 Keys: " + SWbTree.numReads);
        System.out.println("Number of Writes with 1000 Keys: " + SWbTree.numWrites);


        resetStats();
        SWbTree tree3 = new SWbTree();
        System.out.println();


        for(int i = 0; i < 10000; i++) {
            tree3.insert(i);
        }

        System.out.println("Number of Reads with 10000 Keys: " + SWbTree.numReads);
        System.out.println("Number of Writes with 10000 Keys: " + SWbTree.numWrites);


        resetStats();
        SWbTree tree4 = new SWbTree();
        System.out.println();


        for(int i = 0; i < 50000; i++) {
            tree4.insert(i);
        }

        System.out.println("Number of Reads with 50000 Keys: " + SWbTree.numReads);
        System.out.println("Number of Writes with 50000 Keys: " + SWbTree.numWrites);
        System.out.println();
        System.out.println();


        /* Testing average reads per search */
        Random r = new Random();

        //Average reads per search with 100 keys
        System.out.println("Average Number of Reads in swB+ Tree Search");
        int totalReads = 0;
        for(int i = 0; i < 100; i++) {
            resetStats();
            tree1.search(r.nextInt(100));
            totalReads += SWbTree.numReads;
        }

        System.out.println("Average reads for 100 keys: " + (((double)totalReads) / 100.0));

        //Average reads per search with 1000 keys
        totalReads = 0;
        for(int i = 0; i < 100; i++) {
            resetStats();
            tree2.search(r.nextInt(1000));
            totalReads += SWbTree.numReads;
        }

        System.out.println("Average reads for 1000 keys: " + (((double)totalReads) / 100.0));

        //Average reads per search with 10000 keys
        totalReads = 0;
        for(int i = 0; i < 100; i++) {
            resetStats();
            tree3.search(r.nextInt(10000));
            totalReads += SWbTree.numReads;
        }

        System.out.println("Average reads for 10000 keys: " + (((double)totalReads) / 100.0));

        resetStats();


        //Average reads per search with 50000 keys
        totalReads = 0;
        for(int i = 0; i < 100; i++) {
            resetStats();
            tree3.search(r.nextInt(50000));
            totalReads += SWbTree.numReads;
        }

        System.out.println("Average reads for 50000 keys: " + (((double)totalReads) / 100.0));

        resetStats();
        System.out.println();
        System.out.println();



        System.out.println("Read and Writes in swB+ Tree Rebuild");
        /* Number of reads and writes in a rebuild */
        resetStats();
        tree1.rebuild();

        System.out.println("Number of reads to rebuild tree with 100 keys: " + SWbTree.numReads);
        System.out.println("Number of writes to rebuild tree with 100 keys: " + SWbTree.numWrites);
        System.out.println();

        resetStats();
        tree2.rebuild();

        System.out.println("Number of reads to rebuild tree with 1000 keys: " + SWbTree.numReads);
        System.out.println("Number of writes to rebuild tree with 1000 keys: " + SWbTree.numWrites);
        System.out.println();

        resetStats();
        tree3.rebuild();

        System.out.println("Number of reads to rebuild tree with 10000 keys: " + SWbTree.numReads);
        System.out.println("Number of writes to rebuild tree with 10000 keys: " + SWbTree.numWrites);
        System.out.println();

        resetStats();
        tree4.rebuild();

        System.out.println("Number of reads to rebuild tree with 50000 keys: " + SWbTree.numReads);
        System.out.println("Number of writes to rebuild tree with 50000 keys: " + SWbTree.numWrites);
        System.out.println();
    }
}