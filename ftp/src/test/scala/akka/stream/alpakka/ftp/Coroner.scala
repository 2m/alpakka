/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.ftp

import java.io.PrintStream
import java.lang.management.{ManagementFactory, ThreadInfo}
import java.util.Date

object Coroner {

  def printReport(reportTitle: String, out: PrintStream) = {
    import out.println

    val osMx = ManagementFactory.getOperatingSystemMXBean()
    val rtMx = ManagementFactory.getRuntimeMXBean()
    val memMx = ManagementFactory.getMemoryMXBean()
    val threadMx = ManagementFactory.getThreadMXBean()

    println(s"""#Coroner's Report: $reportTitle
               #OS Architecture: ${osMx.getArch()}
               #Available processors: ${osMx.getAvailableProcessors()}
               #System load (last minute): ${osMx.getSystemLoadAverage()}
               #VM start time: ${new Date(rtMx.getStartTime())}
               #VM uptime: ${rtMx.getUptime()}ms
               #Heap usage: ${memMx.getHeapMemoryUsage()}
               #Non-heap usage: ${memMx.getNonHeapMemoryUsage()}""".stripMargin('#'))

    def dumpAllThreads: Seq[ThreadInfo] =
      threadMx.dumpAllThreads(threadMx.isObjectMonitorUsageSupported, threadMx.isSynchronizerUsageSupported)

    def findDeadlockedThreads: (Seq[ThreadInfo], String) = {
      val (ids, desc) = if (threadMx.isSynchronizerUsageSupported()) {
        (threadMx.findDeadlockedThreads(), "monitors and ownable synchronizers")
      } else {
        (threadMx.findMonitorDeadlockedThreads(), "monitors, but NOT ownable synchronizers")
      }
      if (ids == null) {
        (Seq.empty, desc)
      } else {
        val maxTraceDepth = 1000 // Seems deep enough
        (threadMx.getThreadInfo(ids, maxTraceDepth), desc)
      }
    }

    def printThreadInfos(threadInfos: Seq[ThreadInfo]) =
      if (threadInfos.isEmpty) {
        println("None")
      } else {
        for (ti ← threadInfos.sortBy(_.getThreadName)) { println(threadInfoToString(ti)) }
      }

    def threadInfoToString(ti: ThreadInfo): String = {
      val sb = new java.lang.StringBuilder
      sb.append("\"")
      sb.append(ti.getThreadName)
      sb.append("\" Id=")
      sb.append(ti.getThreadId)
      sb.append(" ")
      sb.append(ti.getThreadState)

      if (ti.getLockName != null) {
        sb.append(" on " + ti.getLockName)
      }

      if (ti.getLockOwnerName != null) {
        sb.append(" owned by \"")
        sb.append(ti.getLockOwnerName)
        sb.append("\" Id=")
        sb.append(ti.getLockOwnerId)
      }

      if (ti.isSuspended) {
        sb.append(" (suspended)")
      }

      if (ti.isInNative) {
        sb.append(" (in native)")
      }

      sb.append('\n')

      def appendMsg(msg: String, o: Any) = {
        sb.append(msg)
        sb.append(o)
        sb.append('\n')
      }

      val stackTrace = ti.getStackTrace
      for (i ← 0 until stackTrace.length) {
        val ste = stackTrace(i)
        appendMsg("\tat ", ste)
        if (i == 0 && ti.getLockInfo != null) {
          import java.lang.Thread.State._
          ti.getThreadState match {
            case BLOCKED ⇒ appendMsg("\t-  blocked on ", ti.getLockInfo)
            case WAITING ⇒ appendMsg("\t-  waiting on ", ti.getLockInfo)
            case TIMED_WAITING ⇒ appendMsg("\t-  waiting on ", ti.getLockInfo)
            case _ ⇒
          }
        }

        for (mi ← ti.getLockedMonitors if mi.getLockedStackDepth == i)
          appendMsg("\t-  locked ", mi)
      }

      val locks = ti.getLockedSynchronizers
      if (locks.length > 0) {
        appendMsg("\n\tNumber of locked synchronizers = ", locks.length)
        for (li ← locks) appendMsg("\t- ", li)
      }
      sb.append('\n')
      sb.toString
    }

    println("All threads:")
    printThreadInfos(dumpAllThreads)

    val (deadlockedThreads, deadlockDesc) = findDeadlockedThreads
    println(s"Deadlocks found for $deadlockDesc:")
    printThreadInfos(deadlockedThreads)
  }

}
