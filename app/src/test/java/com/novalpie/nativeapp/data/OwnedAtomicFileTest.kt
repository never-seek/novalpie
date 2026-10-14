package com.novalpie.nativeapp.data

import java.io.File
import org.junit.Test
import org.junit.Assert.*

class OwnedAtomicFileTest {
    @Test fun commitsReplacementWithoutDependingOnOverwriteRename() {
        val f=File.createTempFile("owned-atomic", ".fixture")
        try {
            f.writeText("old")
            val atomic=OwnedAtomicFile(f);val out=atomic.startWrite()
            out.write("new".toByteArray());atomic.finishWrite(out)
            assertEquals("new",OwnedAtomicFile(f).openRead().use {it.readBytes().toString(Charsets.UTF_8)})
            assertFalse(File(f.path+".bak").exists())
        }finally{f.delete();File(f.path+".bak").delete()}
    }
    @Test fun failedWriteRestoresOldBytes() {
        val f=File.createTempFile("owned-atomic", ".fixture")
        try {
            f.writeText("old")
            val atomic=OwnedAtomicFile(f);val out=atomic.startWrite()
            out.write("partial".toByteArray());atomic.failWrite(out)
            assertEquals("old",f.readText())
        }finally{f.delete();File(f.path+".bak").delete()}
    }
    @Test fun newInstanceRollsBackAnInterruptedWrite() {
        val f=File.createTempFile("owned-atomic", ".fixture")
        try {
            f.writeText("old")
            OwnedAtomicFile(f).startWrite().use {it.write("incomplete".toByteArray())}
            assertEquals("old",OwnedAtomicFile(f).openRead().use {it.readBytes().toString(Charsets.UTF_8)})
        }finally{f.delete();File(f.path+".bak").delete()}
    }
}
