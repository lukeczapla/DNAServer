DNA Analysis and Simulation Workbench
==============

INSTALLATION
--------------

 - Check the INSTALL file for installation instructions for the system and its dependencies.


ABOUT THIS APP
--------------

*This is an app created by Luke Czapla in collaboration with Rutgers University.  The code is licensed under GPL version 3.  All code dependencies not part of the Java source are under separate GPL3-compatible licenses (GPL/LGPL, MIT or Apache licenses).  These include Nd4j, Dl4j, BioJava, 3Dmol, ChartJS, Plotly, Numeric.js, and XML2JSON)*

 - This web application allows for running simulations of DNA with the models and methodology published by Czapla, Swigon, and Olson in 2006 and 2008, along with 2013 and more recent ones with the protein binding chemical potentials (Wei et al, PNAS).  Tasks are launched on the server backend, returning a token allowing for later analysis when the job is finished.  Researchers can collaborate by sharing the token and code for their jobs and visualize everything in their web browser with HTML5.  The half-chain combination method (3D grid bucket search) reduces the computational complexity of sampling N^2 chains and finding loops and minicircles to O(N) running time for problems where the states are sufficiently rare.

 - The Java API provides controllers for the client using REST.  Processing is done on both the frontend and backend.  The backend launches the simulation C++ code and also analyzes structures using a new methodology for extracting base pairing and base-pair step parameters (3DNA-like with Curves+-compatible frames).

 - Even without the backend software, the application can still analyze elastic models and calculate the persistance length of a DNA sequence, and simulate DNA locally using two different sampling methodologies (Gaussian random number generator and Metropolis Monte Carlo with a possible biasing function a.k.a. Wang-Landau method)

 - This version does the RCSB analysis using a 3DNA variant that is compatible with EPFL's cgDNA+ models, with routines for the Cayley representations.  This has been merged from another version (DNAServer-EPFL) into a few targetted routines in NDDNA (GPU-accelerated matrix routines)
