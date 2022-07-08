package io.kipp.github.dependency.graph.domain

/** Detector is the representation of the tool used to gather the dependency
  * snapshot information.
  *
  * @param name The name of the detector used
  * @param url The url of the detector used
  * @param version The version of the detector used
  */
final case class Detector(
    name: String,
    url: String,
    version: String
)
