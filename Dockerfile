# Use the official webrtc-streamer image from Docker Hub
FROM mpromonet/webrtc-streamer

# Expose the default port for WebRTC streaming
EXPOSE 8000

# The default command is already defined in the base image,
# so no need to specify CMD or ENTRYPOINT here.
