// This is a generated file! Please edit source .ksy file and use kaitai-struct-compiler to rebuild

package com.insidious.plugin.parser;

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStream;
import io.kaitai.struct.KaitaiStruct;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class ClassWeaveByteFormat extends KaitaiStruct {
    public static ClassWeaveByteFormat fromFile(String fileName) throws IOException {
        return new ClassWeaveByteFormat(new ByteBufferKaitaiStream(fileName));
    }

    public ClassWeaveByteFormat(KaitaiStream _io) {
        this(_io, null, null);
    }

    public ClassWeaveByteFormat(KaitaiStream _io, KaitaiStruct _parent) {
        this(_io, _parent, null);
    }

    public ClassWeaveByteFormat(KaitaiStream _io, KaitaiStruct _parent, ClassWeaveByteFormat _root) {
        super(_io);
        this._parent = _parent;
        this._root = _root == null ? this : _root;
        _read();
    }
    private void _read() {
        this.classCount = this._io.readU4be();
        classInfo = new ArrayList<ClassInfo>(((Number) (classCount())).intValue());
        for (int i = 0; i < classCount(); i++) {
            this.classInfo.add(new ClassInfo(this._io, this, _root));
        }
    }
    public static class ClassInfo extends KaitaiStruct {
        public static ClassInfo fromFile(String fileName) throws IOException {
            return new ClassInfo(new ByteBufferKaitaiStream(fileName));
        }

        public ClassInfo(KaitaiStream _io) {
            this(_io, null, null);
        }

        public ClassInfo(KaitaiStream _io, ClassWeaveByteFormat _parent) {
            this(_io, _parent, null);
        }

        public ClassInfo(KaitaiStream _io, ClassWeaveByteFormat _parent, ClassWeaveByteFormat _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.className = new StrWithLen(this._io, this, _root);
            this.probeCount = this._io.readU4be();
            probeList = new ArrayList<ProbeInfo>(((Number) (probeCount())).intValue());
            for (int i = 0; i < probeCount(); i++) {
                this.probeList.add(new ProbeInfo(this._io, this, _root));
            }
            this.methodCount = this._io.readU4be();
            methodList = new ArrayList<MethodInfo>(((Number) (methodCount())).intValue());
            for (int i = 0; i < methodCount(); i++) {
                this.methodList.add(new MethodInfo(this._io, this, _root));
            }
        }
        private StrWithLen className;
        private long probeCount;
        private ArrayList<ProbeInfo> probeList;
        private long methodCount;
        private ArrayList<MethodInfo> methodList;
        private ClassWeaveByteFormat _root;
        private ClassWeaveByteFormat _parent;
        public StrWithLen className() { return className; }
        public long probeCount() { return probeCount; }
        public ArrayList<ProbeInfo> probeList() { return probeList; }
        public long methodCount() { return methodCount; }
        public ArrayList<MethodInfo> methodList() { return methodList; }
        public ClassWeaveByteFormat _root() { return _root; }
        public ClassWeaveByteFormat _parent() { return _parent; }
    }
    public static class MethodInfo extends KaitaiStruct {
        public static MethodInfo fromFile(String fileName) throws IOException {
            return new MethodInfo(new ByteBufferKaitaiStream(fileName));
        }

        public MethodInfo(KaitaiStream _io) {
            this(_io, null, null);
        }

        public MethodInfo(KaitaiStream _io, ClassInfo _parent) {
            this(_io, _parent, null);
        }

        public MethodInfo(KaitaiStream _io, ClassInfo _parent, ClassWeaveByteFormat _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.classId = this._io.readU4be();
            this.methodId = this._io.readU4be();
            this.methodName = new StrWithLen(this._io, this, _root);
            this.methodDescriptor = new StrWithLen(this._io, this, _root);
            this.access = this._io.readU4be();
            this.hasSourceFileName = this._io.readBitsIntBe(1) != 0;
            this._io.alignToByte();
            if (hasSourceFileName()) {
                this.sourceFileName = new StrWithLen(this._io, this, _root);
            }
            this.hasMethodHash = this._io.readBitsIntBe(1) != 0;
            this._io.alignToByte();
            if (hasMethodHash()) {
                this.methodHash = new StrWithLen(this._io, this, _root);
            }
        }
        private long classId;
        private long methodId;
        private StrWithLen methodName;
        private StrWithLen methodDescriptor;
        private long access;
        private boolean hasSourceFileName;
        private StrWithLen sourceFileName;
        private boolean hasMethodHash;
        private StrWithLen methodHash;
        private ClassWeaveByteFormat _root;
        private ClassInfo _parent;
        public long classId() { return classId; }
        public long methodId() { return methodId; }
        public StrWithLen methodName() { return methodName; }
        public StrWithLen methodDescriptor() { return methodDescriptor; }
        public long access() { return access; }
        public boolean hasSourceFileName() { return hasSourceFileName; }
        public StrWithLen sourceFileName() { return sourceFileName; }
        public boolean hasMethodHash() { return hasMethodHash; }
        public StrWithLen methodHash() { return methodHash; }
        public ClassWeaveByteFormat _root() { return _root; }
        public ClassInfo _parent() { return _parent; }
    }
    public static class ProbeInfo extends KaitaiStruct {
        public static ProbeInfo fromFile(String fileName) throws IOException {
            return new ProbeInfo(new ByteBufferKaitaiStream(fileName));
        }

        public ProbeInfo(KaitaiStream _io) {
            this(_io, null, null);
        }

        public ProbeInfo(KaitaiStream _io, ClassInfo _parent) {
            this(_io, _parent, null);
        }

        public ProbeInfo(KaitaiStream _io, ClassInfo _parent, ClassWeaveByteFormat _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.classId = this._io.readU4be();
            this.methodId = this._io.readU4be();
            this.dataId = this._io.readU4be();
            this.lineNumber = this._io.readU4be();
            this.instructionIndex = this._io.readU4be();
        }
        private long classId;
        private long methodId;
        private long dataId;
        private long lineNumber;
        private long instructionIndex;
        private ClassWeaveByteFormat _root;
        private ClassInfo _parent;
        public long classId() { return classId; }
        public long methodId() { return methodId; }
        public long dataId() { return dataId; }
        public long lineNumber() { return lineNumber; }
        public long instructionIndex() { return instructionIndex; }
        public ClassWeaveByteFormat _root() { return _root; }
        public ClassInfo _parent() { return _parent; }
    }
    public static class StrWithLen extends KaitaiStruct {
        public static StrWithLen fromFile(String fileName) throws IOException {
            return new StrWithLen(new ByteBufferKaitaiStream(fileName));
        }

        public StrWithLen(KaitaiStream _io) {
            this(_io, null, null);
        }

        public StrWithLen(KaitaiStream _io, KaitaiStruct _parent) {
            this(_io, _parent, null);
        }

        public StrWithLen(KaitaiStream _io, KaitaiStruct _parent, ClassWeaveByteFormat _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.len = this._io.readU4be();
            this.value = new String(this._io.readBytes(len()), Charset.forName("UTF-8"));
            this.eventType = new StrWithLen(this._io, this, _root);
            this.valueDescriptor = new StrWithLen(this._io, this, _root);
            this.attributes = new StrWithLen(this._io, this, _root);
        }
        private long len;
        private String value;
        private StrWithLen eventType;
        private StrWithLen valueDescriptor;
        private StrWithLen attributes;
        private ClassWeaveByteFormat _root;
        private KaitaiStruct _parent;
        public long len() { return len; }
        public String value() { return value; }
        public StrWithLen eventType() { return eventType; }
        public StrWithLen valueDescriptor() { return valueDescriptor; }
        public StrWithLen attributes() { return attributes; }
        public ClassWeaveByteFormat _root() { return _root; }
        public KaitaiStruct _parent() { return _parent; }
    }
    private long classCount;
    private ArrayList<ClassInfo> classInfo;
    private ClassWeaveByteFormat _root;
    private KaitaiStruct _parent;
    public long classCount() { return classCount; }
    public ArrayList<ClassInfo> classInfo() { return classInfo; }
    public ClassWeaveByteFormat _root() { return _root; }
    public KaitaiStruct _parent() { return _parent; }
}
