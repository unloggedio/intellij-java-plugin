// This is a generated file! Please edit source .ksy file and use kaitai-struct-compiler to rebuild

package com.insidious.common.parser;

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStream;
import io.kaitai.struct.KaitaiStruct;

import java.io.IOException;
import java.util.ArrayList;

public class KaitaiInsidiousIndexParser extends KaitaiStruct {
    public static KaitaiInsidiousIndexParser fromFile(String fileName) throws IOException {
        return new KaitaiInsidiousIndexParser(new ByteBufferKaitaiStream(fileName));
    }

    public KaitaiInsidiousIndexParser(KaitaiStream _io) {
        this(_io, null, null);
    }

    public KaitaiInsidiousIndexParser(KaitaiStream _io, KaitaiStruct _parent) {
        this(_io, _parent, null);
    }

    public KaitaiInsidiousIndexParser(KaitaiStream _io, KaitaiStruct _parent, KaitaiInsidiousIndexParser _root) {
        super(_io);
        this._parent = _parent;
        this._root = _root == null ? this : _root;
        _read();
    }
    private void _read() {
        this.indexFileCount = this._io.readU4be();
        indexFiles = new ArrayList<IndexedFile>(((Number) (indexFileCount())).intValue());
        for (int i = 0; i < indexFileCount(); i++) {
            this.indexFiles.add(new IndexedFile(this._io, this, _root));
        }
        this.unionValueIdIndexLen = this._io.readU4be();
        this.unionValueIdIndex = this._io.readBytes(unionValueIdIndexLen());
        this.unionProbeIdIndexLen = this._io.readU4be();
        this.unionProbeIdIndex = this._io.readBytes(unionProbeIdIndexLen());
        this.endTime = this._io.readU8be();
    }
    public static class IndexedFile extends KaitaiStruct {
        public static IndexedFile fromFile(String fileName) throws IOException {
            return new IndexedFile(new ByteBufferKaitaiStream(fileName));
        }

        public IndexedFile(KaitaiStream _io) {
            this(_io, null, null);
        }

        public IndexedFile(KaitaiStream _io, KaitaiInsidiousIndexParser _parent) {
            this(_io, _parent, null);
        }

        public IndexedFile(KaitaiStream _io, KaitaiInsidiousIndexParser _parent, KaitaiInsidiousIndexParser _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.filePath = new StrWithLen(this._io, this, _root);
            this.threadId = this._io.readU8be();
            this.valueIdIndexLen = this._io.readU4be();
            this.valueIdIndex = this._io.readBytes(valueIdIndexLen());
            this.probeIdIndexLen = this._io.readU4be();
            this.probeIdIndex = this._io.readBytes(probeIdIndexLen());
        }
        private StrWithLen filePath;
        private long threadId;
        private long valueIdIndexLen;
        private byte[] valueIdIndex;
        private long probeIdIndexLen;
        private byte[] probeIdIndex;
        private KaitaiInsidiousIndexParser _root;
        private KaitaiInsidiousIndexParser _parent;
        public StrWithLen filePath() { return filePath; }
        public long threadId() { return threadId; }
        public long valueIdIndexLen() { return valueIdIndexLen; }
        public byte[] valueIdIndex() { return valueIdIndex; }
        public long probeIdIndexLen() { return probeIdIndexLen; }
        public byte[] probeIdIndex() { return probeIdIndex; }
        public KaitaiInsidiousIndexParser _root() { return _root; }
        public KaitaiInsidiousIndexParser _parent() { return _parent; }
    }
    public static class StrWithLen extends KaitaiStruct {
        public static StrWithLen fromFile(String fileName) throws IOException {
            return new StrWithLen(new ByteBufferKaitaiStream(fileName));
        }

        public StrWithLen(KaitaiStream _io) {
            this(_io, null, null);
        }

        public StrWithLen(KaitaiStream _io, IndexedFile _parent) {
            this(_io, _parent, null);
        }

        public StrWithLen(KaitaiStream _io, IndexedFile _parent, KaitaiInsidiousIndexParser _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.len = this._io.readU4be();
            this.value = this._io.readBytes(len());
        }
        private long len;
        private byte[] value;
        private KaitaiInsidiousIndexParser _root;
        private IndexedFile _parent;
        public long len() { return len; }
        public byte[] value() { return value; }
        public KaitaiInsidiousIndexParser _root() { return _root; }
        public IndexedFile _parent() { return _parent; }
    }
    private long indexFileCount;
    private ArrayList<IndexedFile> indexFiles;
    private long unionValueIdIndexLen;
    private byte[] unionValueIdIndex;
    private long unionProbeIdIndexLen;
    private byte[] unionProbeIdIndex;
    private long endTime;
    private KaitaiInsidiousIndexParser _root;
    private KaitaiStruct _parent;
    public long indexFileCount() { return indexFileCount; }
    public ArrayList<IndexedFile> indexFiles() { return indexFiles; }
    public long unionValueIdIndexLen() { return unionValueIdIndexLen; }
    public byte[] unionValueIdIndex() { return unionValueIdIndex; }
    public long unionProbeIdIndexLen() { return unionProbeIdIndexLen; }
    public byte[] unionProbeIdIndex() { return unionProbeIdIndex; }
    public long endTime() { return endTime; }
    public KaitaiInsidiousIndexParser _root() { return _root; }
    public KaitaiStruct _parent() { return _parent; }
}
