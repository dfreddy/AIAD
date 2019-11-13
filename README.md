# AIAD
## Flight &amp; Crew Distribution: Utility Based Multi Agent System 

[PowerPoint](https://docs.google.com/presentation/d/1yG94WNCDLMswl1D4H3JhwyKTZkUZMM1s5UMv0yAj5R4/edit?usp=sharing)

### Agents
- Crew Members - Airplanes 
 
### Variáveis Dependentes 
- Crew Member: - Tempo de espera (até ao proximo voo) - Preço a cobrar pela viagem (por hora?) - Posição inicial (aeroporto) - Airplanes: - Esperada satisfação dos viajantes (dependente da experiencia da tripulação) 
 
### Variáveis Independentes 
- Crew Member: - Necessidade/Interesse em baixar preço - Airplanes: - Urgência para recrutar - Recursos monetarios disponiveis 
 
### Utility Function (Crew Member) 
Define felicidade do agente com o seu estado final. Vai depender do tempo de espera e preço conseguido para a viagem, tal como a sua preferencia pela duraçao da viagem que irá efetuar.

### Utility Function (Airplane) 
Define felicidade do agente airplane com o voo a efetuar. Vai depender da experiencia da tripulação e recursos monetarios de sobra.
 
### Environment 
Neste sistema multi agentes, os agentes Airplanes têm viagens a efetuar. Um dado tempo antes da viagem ser efetuada, este agente requisita ao mercado membros individuais para a tripulação. Os agentes Crew Member, membros de tripulação individuais com cargos especificos, irão respondendo às propostas existentes, de modo a encontrar o voo que se adequa mais aos seus desejos. Os agentes Airplanes têem a possibilidade de não aceitar imediatamente um crew member, se acreditarem que este não tem experiencia suficiente. Há medida que o tempo vai passando, e que vão surgindo propostas diferentes, as exigencias de ambos agentes vão sendo sugeitas a alterações. Havendo, portanto, um ambiente constante de negociação neste sistema. As negociações entre agentes serão descentralisadas, de tal modo não haverá a existencia de intermediários. 
 
### Strategies 
Num primeiro plano, iremos criar um ambiente isolado no tempo, num só aeroporto, com um só agente Airplanes e diversos Crew Members, de modo a simular a competitividade entre Crew Members. Num segundo plano, adicionaremos mais Airplanes ao sistema, de modo a criar competitividade também entre os agentes Airplanes, dando opções aos Crew Members. Num terceiro estado, o ambiente não estará mais isolado no tempo, havendo portanto um flow constante de Airplanes e Crew Members a entrar e sair do aeroporto. Num estado final, teremos vários sistemas de aeroportos conectados (eg. um Airplane A parte do aeroporto do Porto para o aeroporto de Lisboa, com uma Crew {a, b, c, etc}. Passado 1 hora, estará este mesmo Airplane A e os Crew Members a,b,c,etc inseridos no sistema de negociações do aeroporto de Lisboa
