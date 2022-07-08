package io.kipp.github.dependency.graph.domain

/** Representing the infomation for the manifest file.
  *
  * @param source_location The path of the manifest file relative to the root
  * of the Git repository.
  */
final case class FileInfo(source_location: String)
