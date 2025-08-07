import streamlit as st

col1,col2, col3 = st.columns([1,2, 3])
col1.title('Sum:')

with st.form('addition', clear_on_submit=True):
    a = st.number_input('a', key="valA")
    b = st.number_input('b')
    submit = st.form_submit_button('add')
    col3.title(f'{a+b:.2f}')

if submit:
    col2.title(f'{a+b:.2f}')

st.text(st.session_state["valA"])

st.slider("lol refresh")
