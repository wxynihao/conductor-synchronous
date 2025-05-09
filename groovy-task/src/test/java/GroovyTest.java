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
import java.util.HashMap;
import java.util.Map;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class GroovyTest {

    public static void main(String[] args) {
        String expression =
                "\"123^456\".split('\\\\^').collect { id -> [redis_query: [cacheName: 'data.hdfs.cache', command: 'hget 日游_' + id + ' score']] }";
        Map<String, Object> idStr = new HashMap<>();
        idStr.put("idStr", "123^234");
        Binding binding = new Binding();
        binding.setVariable("input", idStr);
        GroovyShell shell = new GroovyShell(binding);
        Object result = shell.evaluate(expression);
        System.out.println("aaaaaaaaaa:" + result);
    }
}
