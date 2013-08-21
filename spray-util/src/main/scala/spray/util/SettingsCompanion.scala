/*
 * Copyright (C) 2011-2013 spray.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spray.util

import akka.actor.ActorSystem
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory._
import scala.collection.immutable.ListMap

abstract class SettingsCompanion[T](prefix: String) {
  private final val MaxCached = 8
  private[this] var cache = ListMap.empty[ActorSystem, T]
  private[this] val lock = new AnyRef

  def apply(system: ActorSystem): T =
    cache.getOrElse(system, {
      val settings = apply(system.settings.config)
      lock.synchronized {
        val c =
          if (cache.size < MaxCached) cache
          else cache.tail // drop the first (and oldest) cache entry
        cache = c.updated(system, settings)
      }
      settings
    })

  def apply(configOverrides: String): T =
    apply(parseString(configOverrides)
      .withFallback(Utils.sprayConfigAdditions)
      .withFallback(defaultReference(getClass.getClassLoader)))

  def apply(config: Config): T =
    fromSubConfig(config getConfig prefix)

  def fromSubConfig(c: Config): T
}

