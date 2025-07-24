import streamlit as st
import time

# Initialize session state
if "clicks" not in st.session_state:
    st.session_state.clicks = 0

if "details_shown" not in st.session_state:
    st.session_state.details_shown = 0

# Title and intro
st.title("Jeamlit Demo App")
st.text("Welcome to Jeamlit - Streamlit for Java!")
st.text("This demo shows basic components and state management.")

# Slider
age = st.slider("Select your age", min_value=0, max_value=100, value=30)
st.text(f"You selected age: {age}")
st.text(f"Age category: {'Minor' if age < 18 else 'Adult' if age < 65 else 'Senior'}")

# Button with click count
if st.button("Click me!"):
    st.session_state.clicks += 1
    st.text("Button was clicked!")
    st.text(f"Button clicked {st.session_state.clicks} times")

# Show details button
if st.button("Show details"):
    st.text("🎯 This is a detailed view!")
    st.text(f"Current timestamp: {int(time.time() * 1000)}")
    st.session_state.details_shown += 1
    st.text(f"Details shown {st.session_state.details_shown} times")

# Footer
st.text("---")
st.text("💡 Try changing values and see the app update in real-time!")

