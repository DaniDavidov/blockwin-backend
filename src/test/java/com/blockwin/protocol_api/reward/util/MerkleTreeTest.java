package com.blockwin.protocol_api.reward.util;

import org.junit.jupiter.api.Test;
import org.web3j.crypto.Hash;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MerkleTreeTest {

    /** Creates a 32-byte leaf whose last byte is {@code value}. */
    private static byte[] leaf(int value) {
        byte[] l = new byte[32];
        l[31] = (byte) value;
        return l;
    }

    /** Replicates MerkleTree.hashPair for proof reconstruction in tests. */
    private static byte[] hashPair(byte[] a, byte[] b) {
        int cmp = compareUnsigned(a, b);
        byte[] combined = (cmp <= 0) ? concat(a, b) : concat(b, a);
        return Hash.sha3(combined);
    }

    private static int compareUnsigned(byte[] a, byte[] b) {
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int cmp = Byte.compareUnsigned(a[i], b[i]);
            if (cmp != 0) return cmp;
        }
        return Integer.compare(a.length, b.length);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }


    @Test
    void singleLeaf_rootEqualsLeaf() {
        byte[] l = leaf(1);
        assertArrayEquals(l, new MerkleTree(List.of(l)).getRoot());
    }

    @Test
    void singleLeaf_proofIsEmpty() {
        byte[] l = leaf(1);
        assertTrue(new MerkleTree(List.of(l)).getProof(l).isEmpty());
    }


    @Test
    void twoLeaves_rootMatchesHashPairOfSortedLeaves() {
        byte[] a = leaf(1);
        byte[] b = leaf(2); // a < b unsigned
        byte[] expected = hashPair(a, b);
        assertArrayEquals(expected, new MerkleTree(List.of(a, b)).getRoot());
    }

    @Test
    void twoLeaves_proofContainsSibling() {
        byte[] a = leaf(1);
        byte[] b = leaf(2);
        MerkleTree tree = new MerkleTree(List.of(a, b));

        List<byte[]> proofA = tree.getProof(a);
        assertEquals(1, proofA.size());
        assertArrayEquals(b, proofA.get(0));

        List<byte[]> proofB = tree.getProof(b);
        assertEquals(1, proofB.size());
        assertArrayEquals(a, proofB.get(0));
    }

    @Test
    void twoLeaves_proofReconstructsRoot() {
        byte[] a = leaf(10);
        byte[] b = leaf(20);
        MerkleTree tree = new MerkleTree(List.of(a, b));

        List<byte[]> proof = tree.getProof(a);
        assertArrayEquals(tree.getRoot(), hashPair(a, proof.get(0)));
    }


    @Test
    void threeLeaves_oddNodeIsSelfPairedInProof() {
        byte[] a = leaf(1);
        byte[] b = leaf(2);
        byte[] c = leaf(3);
        MerkleTree tree = new MerkleTree(List.of(a, b, c));

        // c is at index 2 (after sorting); its sibling index 3 is out of bounds → self-paired
        List<byte[]> proofC = tree.getProof(c);
        assertEquals(2, proofC.size());
        assertArrayEquals(c, proofC.get(0));               // self-pair at leaf level
        assertArrayEquals(hashPair(a, b), proofC.get(1)); // sibling at next level
    }

    @Test
    void threeLeaves_proofForFirstLeafReconstructsRoot() {
        byte[] a = leaf(1);
        byte[] b = leaf(2);
        byte[] c = leaf(3);
        MerkleTree tree = new MerkleTree(List.of(a, b, c));

        // Reconstruct manually: sibling of a is b, then go up
        List<byte[]> proof = tree.getProof(a);
        assertEquals(2, proof.size());
        byte[] parent = hashPair(a, proof.get(0));
        byte[] root   = hashPair(parent, proof.get(1));
        assertArrayEquals(tree.getRoot(), root);
    }


    @Test
    void fourLeaves_proofLengthIsTwo() {
        MerkleTree tree = new MerkleTree(List.of(leaf(1), leaf(2), leaf(3), leaf(4)));
        assertEquals(2, tree.getProof(leaf(1)).size());
    }


    @Test
    void sortingMakesTreeDeterministicRegardlessOfInsertionOrder() {
        byte[] a = leaf(1);
        byte[] b = leaf(2);
        byte[] c = leaf(3);

        MerkleTree t1 = new MerkleTree(List.of(a, b, c));
        MerkleTree t2 = new MerkleTree(List.of(c, a, b));
        MerkleTree t3 = new MerkleTree(List.of(b, c, a));

        assertArrayEquals(t1.getRoot(), t2.getRoot());
        assertArrayEquals(t1.getRoot(), t3.getRoot());
    }


    @Test
    void getRootHex_returnsTwosixsixCharPrefixedString() {
        String hex = new MerkleTree(List.of(leaf(1))).getRootHex();
        assertTrue(hex.startsWith("0x"));
        assertEquals(66, hex.length()); // "0x" + 64 hex chars
    }

    @Test
    void getProofHex_allElementsArePrefixedAndCorrectLength() {
        MerkleTree tree = new MerkleTree(List.of(leaf(1), leaf(2)));
        List<String> hexProof = tree.getProofHex(leaf(1));
        assertEquals(1, hexProof.size());
        assertTrue(hexProof.get(0).startsWith("0x"));
        assertEquals(66, hexProof.get(0).length());
    }

    @Test
    void toHex_convertsKnownBytesCorrectly() {
        byte[] bytes = new byte[]{(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
        assertEquals("deadbeef", MerkleTree.toHex(bytes));
    }


    @Test
    void leafNotFound_throwsIllegalArgumentException() {
        MerkleTree tree = new MerkleTree(List.of(leaf(1), leaf(2)));
        assertThrows(IllegalArgumentException.class, () -> tree.getProof(leaf(99)));
    }
}