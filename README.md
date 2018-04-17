## Kotlin Tensorflow image segmentation POC

April 2018.

What we're testing here:

How to take a TensorFlow neural network graph, in this case the TensorFlow Models research
model for DeepLab image segmentation, and incorporate it into a JVM application built on 
Java 10, Kotlin, Spring Boot, Vue.js, jQuery and Bootstrap.

### TL;DR

For now, use the TensorFlow Serving architecture through a gRPC or REST interface, 
to include TensorFlow into your JVM application architecture.

Use `git lfs` when pushing model files to GitHub.

### Lessons learned

As always, TensorFlow is very finicky about its support libraries.

The Google-built TensorFlow 1.7.0 version in Maven Central is built for CUDA 9.0, cuDNN 7.0,
and the CPU module wasn't compiled with the same optimizations that are now standard with the
Python module.

For production use, you'd absolutely want to do your own series of builds of the 
`libtensorflow_jni` and `libtensorflow_jni_gpu` libraries and JARs for a matrix of 
CUDA 9.0 and 9.1, cuDNN 7.0 and 7.1, AVX, AVX2, and Intel MKL. Plus a custom JNI module 
which loads the correct combination for what you have installed on your actual machine.

One could say that you could just standardize your organization on certain versions, but that's
easier said than done, given how fast the libraries are developing, and the need for almost all
user organizations to be a part of the developer landscape at this point of time.

The current JVM abstraction for TensorFlow graph instantiation requires you to read the whole
Protocol Buffer graph file in memory before instantiation, which might be a problem for certain
situations in which you don't actually need a large heap, since all significant processing gets
done in GPU memory, but since your graph file is > 500MB, you must set `-Xmx1g`.

For this use case, go with the MobileNetV2 models, since their resource to performance ratio
is currently best. 

Nowhere mentioned in the TensorFlow for Java documentation is the `org.tensorflow:proto:1.7.0` 
Maven JAR, which contains the dependencies with Google Protocol Buffers that are required for
TF session configuration and reading any response metadata.

In `SegmentationService.start` I'm attempting to disable the use of the GPU through the session
configuration options, let's see how that goes. Worst case, do it through environmental variables.

... moar hear ...

### Hopes and dreams

Since Ubuntu 18.04 LTS is very soon coming out, I'm hoping that Google and NVidia will base their
libraries on that, and we'll kind of get a reasonable baseline for all the different kinds of
libraries that are required for actual application development.

It would be nice if the official libraries shipped in Maven JARs would be compiled with the same
optimizations as the Python libraries.

`org.tensorflow.Graph.importGraphDef(byte[])` should also support import from a stream, or an
off-heap bytebuffer.

### Installation

Requires SPECIFICALLY these library versions:

CUDA 9.0 (**not** 8.0, **not** 9.1)

cuDNN 7.0 (**not** 7.1)

IF YOU DO NOT USE THESE SPECIFIC LIBRARY VERSIONS, THE APP WILL NOT WORK.

See:

```
sudo apt-get install -y --allow-downgrades libcudnn7-dev=7.0.5.15-1+cuda9.0 libcudnn7=7.0.5.15-1+cuda9.0
```