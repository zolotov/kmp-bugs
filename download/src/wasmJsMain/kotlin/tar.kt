import js.typedarrays.toByteArray

internal suspend fun BenchmarkScope.tarLoad() {
  val bytes = downloadFileIntoBytes("/fonts.tar")
  parseTar(bytes.toByteArray())
}

internal suspend fun BenchmarkScope.tarLoadBytesWasmMemory() {
  val bytes = downloadFileIntoBytes("/fonts.tar")
  val byteArray = fastArrayToByteArray(bytes)
  parseTar(byteArray)
}

/**
 * Parses an uncompressed TAR archive from a [ByteArray] and returns a map
 * of relative file paths to their content as [ByteArray].
 *
 * Directories and special entries are skipped — only regular files are returned.
 */
private fun BenchmarkScope.parseTar(tar: ByteArray): Map<String, ByteArray> {
  return span("parseTar") {
    var offset = 0
    buildMap {
      while (offset + 512 <= tar.size) {
        val header = tar.copyOfRange(offset, offset + 512)

        // An all-zero header signals end of archive
        if (header.all { it == 0.toByte() }) break

        val name = header.readTarString(0, 100)
        val sizeStr = header.readTarString(124, 12)
        val typeFlag = header[156]
        val prefix = header.readTarString(345, 155)

        val fileSize = sizeStr.toLongOrNull(8) ?: 0L

        val fullName = if (prefix.isNotEmpty()) "$prefix/$name" else name

        offset += 512 // move past header

        // Type '0' or '\0' (NUL) means regular file
        val isRegularFile = typeFlag == '0'.code.toByte() || typeFlag == 0.toByte()
        if (isRegularFile && fileSize > 0) {
          put(fullName.removePrefix("./"), tar.copyOfRange(offset, offset + fileSize.toInt()))
        }

        // Data blocks are padded to 512-byte boundaries
        offset += ((fileSize + 511) / 512 * 512).toInt()
      }
    }
  }
}

/**
 * Reads a null/space-terminated ASCII string from a TAR header field.
 */
private fun ByteArray.readTarString(offset: Int, maxLen: Int): String {
    var end = offset
    val limit = offset + maxLen
    while (end < limit && this[end] != 0.toByte()) end++
    return decodeToString(offset, end).trimEnd(' ')
}
