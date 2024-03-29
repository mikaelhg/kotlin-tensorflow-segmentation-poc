var app;

class MaskedImage {

    constructor(imageId, maskId) {
        this.imageId = imageId;
        this.maskId = maskId;
    }

    get imageDomId() {
        return `image-${this.imageId}`;
    }

    get maskDomId() {
        return `mask-${this.maskId}`;
    }

    get combinedUrl() {
        return `/show/combined/${this.imageId}/${this.maskId}`;
    }

    get imageUrl() {
        return `/show/image/${this.imageId}`;
    }

    get maskUrl() {
        return `/show/mask/${this.maskId}`;
    }

    get style() {
        return `width: 720px;`;
    }

}

Dropzone.options.myAwesomeDropzone = {
    init: function() {
        this.on("addedfile", function(file) { console.log("Added file."); });
        this.on("success", function (file, response) {
            app.images.unshift(new MaskedImage(response.imageId, response.maskId));
        });
    }
};

$(function() {
    console.log("ready!");

    app = Vue.createApp({
        data() {
            return { images: [] }
        }
    }).mount('#app');

});
