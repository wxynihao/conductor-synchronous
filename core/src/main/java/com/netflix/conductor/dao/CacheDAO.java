/*
 * Copyright 2025 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.dao;

public interface CacheDAO {

    /**
     * 读取缓存
     *
     * @param key 缓存的key
     * @return 缓存里的值
     */
    String get(String key);

    /**
     * 写入缓存
     *
     * @param key 缓存的key
     * @param value 缓存字符串
     * @param ttlSeconds 缓存过期时间，单位秒
     */
    boolean set(String key, String value, int ttlSeconds);

    /**
     * 异步写入缓存
     *
     * @param key 缓存的key
     * @param value 缓存字符串
     * @param ttlSeconds 缓存过期时间，单位秒
     */
    void asyncSet(String key, String value, int ttlSeconds);
}
