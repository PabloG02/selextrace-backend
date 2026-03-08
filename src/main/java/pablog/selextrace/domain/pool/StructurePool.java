package pablog.selextrace.domain.pool;

import java.io.Serializable;
import java.util.Map.Entry;

/// API for handling the structural data associated with an aptamer pool.
///
/// Implementing classes are responsible for managing the data structures
/// used to encode structural information of aptamers, represented as
/// `double[]` arrays. Implementations may use in-memory or persistent
/// storage backends, and should document which optional operations they support.
///
/// @author Jan Hoinka
/// @see AptamerPool
public interface StructurePool extends Serializable {

    /// Registers the predicted structure for the aptamer identified by the given ID.
    ///
    /// @param id        the unique identifier of the aptamer whose structure is being registered
    /// @param structure the structural encoding of the aptamer as a `double[]` array;
    ///                  must not be `null`
    void registerStructure(int id, double[] structure);


    /// Retrieves the structural information for the aptamer identified by the given ID.
    ///
    /// @param id the unique identifier of the aptamer whose structure is to be retrieved
    /// @return the structural encoding as a `double[]` array,
    ///         or `null` if no structure has been registered for the given ID
    double[] getStructure(int id);

    /// Releases any resources held by this instance, such as file handles.
    ///
    /// @implSpec If no external resources are used, provide an empty method body.
    void close();

    /// Switches the underlying storage to read-only mode, preventing further
    /// modifications to the structure pool.
    ///
    /// @implSpec If access-mode switching is not supported, provide an empty method body.
    void setReadOnly();

    /// Switches the underlying storage to read/write mode, allowing structures
    /// to be registered or updated.
    ///
    /// @implSpec If access-mode switching is not supported, provide an empty method body.
    void setReadWrite();

    /// Persists any buffered or in-memory structures to the underlying storage backend.
    ///
    /// @implSpec If persistent storage is not used, or writes are committed immediately
    ///           on registration, provide an empty method body.
    void commitStructures();

    /// Returns an [Iterable] over all entries in the pool, where each entry
    /// maps an aptamer ID ([Integer]) to its structural encoding (`double[]`).
    ///
    /// The order of iteration is implementation-dependent and should not be
    /// relied upon.
    ///
    /// @return an `Iterable` of `Map.Entry<Integer, double[]>` pairs
    ///         representing all registered aptamer structures
    Iterable<Entry<Integer, double[]>> iterator();

    /// Returns an [Iterable] over all entries in the pool, where each entry
    /// maps an aptamer sequence (`byte[]`) to its structural encoding (`double[]`).
    ///
    /// The order of iteration is implementation-dependent and should not be
    /// relied upon.
    ///
    /// @return an `Iterable` of `Map.Entry<byte[], double[]>` pairs
    ///         representing all registered aptamer structures keyed by sequence
    Iterable<Entry<byte[], double[]>> sequence_iterator();

    /// Returns the total number of structures currently registered in the pool.
    ///
    /// @return the count of registered aptamer structures; never negative
    int size();
}