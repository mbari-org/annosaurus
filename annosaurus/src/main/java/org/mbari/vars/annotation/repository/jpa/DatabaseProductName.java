/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.vars.annotation.repository.jpa;

import com.typesafe.config.ConfigFactory;

public class DatabaseProductName {

  private static String name;

  public static String POSTGRESQL = "PostgreSQL";
  public static String SQLSERVER = "SQLServer";
  public static String DERBY = "Derby";
  public static String ORACLE = "Oracle";

  public static String name() {
    if (name == null) {
      var config = ConfigFactory.load();
      var environment = config.getString("database.environment");
      var nodeName = environment.equalsIgnoreCase("production") ?
        "org.mbari.vars.annotation.database.production" :
         "org.mbari.vars.annotation.database.development";
      name = config.getString(nodeName + ".name");
    }
    return name;
  }

  public static void usePostgreSQL() {
    name = POSTGRESQL;
  }

  public static void useSQLServer() {
    name = SQLSERVER;
  }

  public static void useDerby() {
    name = DERBY;
  }

  public static void useOracle() {
    name = ORACLE;
  }

  public static boolean isPostgreSQL() {
    return name().equalsIgnoreCase(POSTGRESQL);
  }

  public static boolean isSQLServer() {
    return name().equalsIgnoreCase(SQLSERVER);
  }

  public static boolean isDerby() {
    return name().equalsIgnoreCase(DERBY);
  }

  public static boolean isOracle() {
    return name().toLowerCase().startsWith(ORACLE.toLowerCase());
  }
  
}
