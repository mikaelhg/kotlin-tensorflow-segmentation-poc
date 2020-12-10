## Kotlin Tensorflow image segmentation POC

This POC was originally built in April 2018, but it was revised in December 2020
to support the new Tensorflow Java API and Tensorflow 2.3.1.

What we're testing here:

How to take a TensorFlow neural network graph, in this case the TensorFlow Models research
model for DeepLab image segmentation, and incorporate it into a JVM application built on 
Java 15, Kotlin, Spring Boot, Vue.js, jQuery and Bootstrap.

### TL;DR

For now, use the TensorFlow Serving architecture through a gRPC or REST interface, 
to include TensorFlow into your JVM application architecture.

Use `git lfs` when pushing model files to GitHub.

### Lessons learned

#### December 2020

The new Tensorflow Java API required only slight changes to the application code.

The actual inference calls are much faster with the new Tensorflow versions, but
the memory requirements for loading a graph grew considerably.

#### April 2018

As always, TensorFlow is very finicky about its support libraries.

The Google-built TensorFlow 1.15.0 version in Maven Central is built for CUDA 10.0, cuDNN 7.5,
and the CPU module wasn't compiled with the same optimizations that are now standard with the
Python module.

For production use, you'd absolutely want to do your own series of builds of the 
`libtensorflow_jni` and `libtensorflow_jni_gpu` libraries and JARs for a matrix of 
CUDA and cuDNN library versions, AVX, AVX2, and Intel MKL. Then leverage `LD_LIBRARY_PATH`
to select the correct CUDA and cuDNN library versions when running your application.

One could say that you could just standardize your organization on certain versions, but that's
easier said than done, given how fast the libraries are developing, and the need for almost all
user organizations to be a part of the developer landscape at this point of time.

The current JVM abstraction for TensorFlow graph instantiation requires you to read the whole
Protocol Buffer graph file in memory before instantiation, which might be a problem for certain
situations in which you don't actually need a large heap, since all significant processing gets
done in GPU memory, but since your graph file is > 500MB, you must set `-Xmx1g`.

For this use case, go with the MobileNetV2 models, since their resource to performance ratio
is currently best. 

Nowhere mentioned in the TensorFlow for Java documentation is the `org.tensorflow:proto:1.15.0`
Maven JAR, which contains the dependencies with Google Protocol Buffers that are required for
TF session configuration and reading any response metadata.

In `SegmentationService.start` I'm attempting to disable the use of the GPU through the session
configuration options, let's see how that goes. Worst case, do it through environmental variables.

... moar hear ...

### Hopes and dreams

☑ Since Ubuntu 18.04 LTS is very soon coming out, I'm hoping that Google and NVidia will base their
libraries on that, and we'll kind of get a reasonable baseline for all the different kinds of
libraries that are required for actual application development.

☐ It would be nice if the official libraries shipped in Maven JARs would be compiled with the same
optimizations as the Python libraries.

☐ `org.tensorflow.Graph.importGraphDef(byte[])` should also support import from a stream, or an
off-heap bytebuffer.

### Build

```
DOCKER_BUILDKIT=1 docker build -t docker.mikael.io/mikaelhg/kotlin-tensorflow-segmentation-poc:1.0.0 .
```

### Run

```
docker run --gpus all -it --rm --shm-size=1g --ulimit memlock=-1 --ulimit stack=67108864 \
  docker.mikael.io/mikaelhg/kotlin-tensorflow-segmentation-poc:1.0.0
```

### Run locally

Check out https://mikael.io/post/cuda-install/ for your CUDA and cuDNN installation.

```bash
LD_LIBRARY_PATH=/usr/local/cuda-10.0.130/lib64:/usr/local/cudnn-10.0-7.4.2.24/lib64 \
  java -jar target/kotlin-tensorflow-segmentation-poc-1.0.0-gpu.jar
```

### Installation

By default, the POC runs in CPU mode, which is much, much slower than the GPU mode,
which can be switched on by commenting the CPU section and uncommenting the GPU
section in the `pom.yml` file.

Look in the `pom.yml` file to see which version of Tensorflow is being used, then
go to the Tensorflow installation site, and make sure that you have very specifically
the mentioned MAJOR.MINOR version of all the specified libraries installed. If you have
anything else than those specific versions installed, it's very likely that the 
application just won't work. That's Tensorflow for you.
