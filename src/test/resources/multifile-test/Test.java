/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
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


import model.Car;
import model.Owner;import tech.catheu.jeamlit.core.Jt;

public class Test {

    public static void main(String[] args) throws InterruptedException {
        Jt.text(String.valueOf(Car.BLUE)).use();
        Jt.text(String.valueOf(Owner.ME)).use();
    }
}
