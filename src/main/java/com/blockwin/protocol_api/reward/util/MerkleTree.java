package com.blockwin.protocol_api.reward.util;

import org.web3j.crypto.Hash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Binary Merkle tree implementation compatible with OpenZeppelin's MerkleProof.sol.
 *
 * <p>Construction rules (matching OpenZeppelin's MerkleTree.js / MerkleProof.sol):
 * <ul>
 *   <li>Leaves are sorted lexicographically (unsigned byte comparison) before tree construction,
 *       making the tree deterministic regardless of insertion order.</li>
 *   <li>Each internal node is produced by sorting its two children before hashing:
 *       {@code keccak256(min(left, right) || max(left, right))}.
 *       This makes individual proof elements order-independent on the verifier side.</li>
 *   <li>When a level has an odd number of nodes, the trailing node is paired with itself
 *       (duplicated), so every level contributes exactly one proof element per path step.</li>
 * </ul>
 *
 * <p>Leaf hashing is the caller's responsibility. Pass pre-hashed 32-byte leaf values.
 */
public class MerkleTree {

    private final List<byte[]> sortedLeaves;
    private final List<List<byte[]>> layers;

    public MerkleTree(List<byte[]> leaves) {
        sortedLeaves = new ArrayList<>(leaves);
        sortedLeaves.sort(MerkleTree::compareUnsigned);
        layers = buildLayers(sortedLeaves);
    }

    /** Returns the 32-byte Merkle root. */
    public byte[] getRoot() {
        if (layers.isEmpty() || layers.get(layers.size() - 1).isEmpty()) {
            return new byte[32];
        }
        return layers.get(layers.size() - 1).get(0);
    }

    /** Returns the Merkle root as a 0x-prefixed hex string. */
    public String getRootHex() {
        return "0x" + toHex(getRoot());
    }

    /**
     * Returns the Merkle proof (ordered list of sibling hashes) for {@code leaf}.
     * Each element is a 32-byte array.
     *
     * @throws IllegalArgumentException if the leaf is not present in the tree
     */
    public List<byte[]> getProof(byte[] leaf) {
        int index = indexOf(leaf);
        if (index == -1) {
            throw new IllegalArgumentException("Leaf not found in Merkle tree");
        }

        List<byte[]> proof = new ArrayList<>();
        for (int level = 0; level < layers.size() - 1; level++) {
            List<byte[]> layer = layers.get(level);
            boolean isLeftChild = (index % 2 == 0);
            int siblingIdx = isLeftChild ? index + 1 : index - 1;

            // When the node is the last in an odd-length layer it was self-paired;
            // its sibling is itself.
            if (siblingIdx >= layer.size()) {
                proof.add(layer.get(index));
            } else {
                proof.add(layer.get(siblingIdx));
            }
            index /= 2;
        }
        return proof;
    }

    /** Returns the proof as a list of 0x-prefixed hex strings. */
    public List<String> getProofHex(byte[] leaf) {
        return getProof(leaf).stream()
                .map(b -> "0x" + toHex(b))
                .toList();
    }


    private static List<List<byte[]>> buildLayers(List<byte[]> sortedLeaves) {
        List<List<byte[]>> layers = new ArrayList<>();
        List<byte[]> current = new ArrayList<>(sortedLeaves);
        layers.add(current);

        while (current.size() > 1) {
            List<byte[]> next = new ArrayList<>();
            int size = current.size();
            for (int i = 0; i < size; i += 2) {
                byte[] left  = current.get(i);
                byte[] right = (i + 1 < size) ? current.get(i + 1) : left; // self-pair if odd
                next.add(hashPair(left, right));
            }
            layers.add(next);
            current = next;
        }
        return layers;
    }

    /**
     * Produces {@code keccak256(min(a,b) || max(a,b))}, matching Solidity's
     * {@code _hashPair} in OpenZeppelin's MerkleProof.sol.
     */
    private static byte[] hashPair(byte[] a, byte[] b) {
        byte[] combined;
        if (compareUnsigned(a, b) <= 0) {
            combined = concat(a, b);
        } else {
            combined = concat(b, a);
        }
        return Hash.sha3(combined);
    }


    private int indexOf(byte[] leaf) {
        for (int i = 0; i < sortedLeaves.size(); i++) {
            if (Arrays.equals(sortedLeaves.get(i), leaf)) return i;
        }
        return -1;
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
        System.arraycopy(a, 0, result, 0,        a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
