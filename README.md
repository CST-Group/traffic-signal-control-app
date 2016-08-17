# traffic-signal-control-app

CST Machine Consciousness Traffic Signal Control Application designed and implemented in the [PhD Thesis - A machine consciousness approach to urban traffic signal control] (https://www.researchgate.net/publication/304076563_A_machine_consciousness_approach_to_urban_traffic_signal_control?channel=doi&linkId=57657a4d08ae421c4489d260&showFulltext=true).

## Model of the controllers built on top of [CST] (https://github.com/CST-Group/cst)

In order to explain the modeling of the traffic controllers we implemented, we use the simplified network model of the Figure below: ![Network model](/network.png)

In the Figure abov, we detail the many sensed regions, represented by letters (a) to (k), and the different possible phases, drawn on top right, which can be chosen by the controller in these junctions.

The controllers built with CST can be modeled for this simplified situation as shown in the Figure below

![Model of the controllers](/CST-TrafficLightsController.png)

Four types of codelets were used. Three types were implemented for this specific application - a sensory codelet, a behavioral codelet and a motor codelet. Also, the consciousness codelet provided by CST (also a contribution of this work) was used in the model. In the example shown above, junction East is chosen by the consciousness codelet to gain the spotlight. The consciousness codelet does not change the information of the memory object of the junction East in the motor memory, but just highlights it as the conscious content. The output of the behavioral codelet junction East is then broadcasted to the global input B of the behavioral codelet junction West.

For more details, please refer to the [PhD Thesis - A machine consciousness approach to urban traffic signal control] (https://www.researchgate.net/publication/304076563_A_machine_consciousness_approach_to_urban_traffic_signal_control?channel=doi&linkId=57657a4d08ae421c4489d260&showFulltext=true).

## License

    Copyright 2016 CST-Group

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
