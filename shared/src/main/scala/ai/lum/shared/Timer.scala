package ai.lum.shared

object Timer {
  /**
    * Execute a block, returning the results and the time elapsed, measured in nanoseconds
    * @param block A block of executable code
    * @tparam R The type of the result of the block
    */
  def time[R](block: => R): (R, Long) = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    (result, t1 - t0)
  }
}
