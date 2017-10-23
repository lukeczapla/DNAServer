DNA Analysis and Simulation Workbench
==============

INSTALLATION
--------------

 - Check the INSTALL file for installation instructions for the system and its dependencies.


ABOUT THIS APP
--------------

*This is an app created by Luke Czapla at The Frisch School in collaboration with Rutgers University.  The code is licensed under GPL version 3.  All code dependencies not part of the Java source are under separate GPL3-compatible licenses (GPL/LGPL, MIT or Apache licenses).  These include Nd4j, Dl4j, BioJava, 3Dmol, ChartJS, Plotly, Numeric.js, and XML2JSON)*

 - This web application allows for running simulations of DNA with the models and methodology published by Czapla, Swigon, and Olson in 2006 and 2008.  Tasks are launched on the server backend, returning a token allowing for later analysis when the job is finished.  Researchers can collaborate by sharing the token and code for their jobs and visualize everything in their web browser with HTML5.

 - The Java API provides controllers for the client using REST.  Processing is done on both the frontend and backend.  The backend launches the simulation C++ code and also analyzes structures using a new methodology for extracting base pairing and base-pair step parameters.

 - Even without the backend software, the application can still analyze elastic models and calculate the persistance length of a DNA sequence, and simulate DNA locally using two different sampling methodologies (Gaussian random number generator and Metropolis Monte Carlo with a possible biasing function a.k.a. Wang-Landau method)


